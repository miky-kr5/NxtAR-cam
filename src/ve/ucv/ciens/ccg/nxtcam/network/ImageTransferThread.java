/*
 * Copyright (C) 2013 Miguel Angel Astor Romero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.camera.CameraImageMonitor;
import ve.ucv.ciens.ccg.nxtcam.network.protocols.ImageDataMessage;
import ve.ucv.ciens.ccg.nxtcam.network.protocols.ImageTransferProtocol;
import ve.ucv.ciens.ccg.nxtcam.network.protocols.ImageTransferProtocolMessage;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

public class ImageTransferThread extends Thread{
	private final String TAG = "IM_THREAD";
	private final String CLASS_NAME = ImageTransferThread.class.getSimpleName();

	private enum thread_state_t {WAIT_FOR_ACK, WAIT_FOR_READY, CAN_SEND, END_STREAM};

	private boolean pause, done;
	private Object threadPauseMonitor;
	private CameraImageMonitor camMonitor;
	private Socket socket;
	private ObjectOutputStream writer;
	private ObjectInputStream reader;	private String serverIp;
	private thread_state_t threadState;

	public ImageTransferThread(String serverIp){
		this.serverIp = serverIp;
		pause = false;
		done = false;
		threadPauseMonitor = new Object();
		socket = null;
		writer = null;
		reader = null;
		camMonitor = CameraImageMonitor.getInstance();
		threadState = thread_state_t.WAIT_FOR_READY;
	}

	public void run(){
		byte[] image;
		Object auxiliary;
		ImageTransferProtocolMessage simpleMessage;
		ImageDataMessage imageMessage;
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		connectToServer();

		if(!socket.isConnected()){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Not connected to a server. Finishing thread.");
			return;
		}else{
			while(!done){
				// checkPause();
				switch(threadState){
				case WAIT_FOR_READY:
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Reading message from server. State is WAIT_FOR_READY.");
					auxiliary = readMessage();

					if(!validateImageTransferProtocolMessage(auxiliary)){
						// If the message received is not valid then send an UNRECOGNIZED message to the server.
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Received an unrecognized protocol message. State WAIT_FOR_READY.");
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending UNRECOGNIZED message to server.");
						sendUnrecognizedMessage();

					}else{
						// Else if the message passed the validity check then proceed to the next protocol state.
						simpleMessage = (ImageTransferProtocolMessage)auxiliary;
						if(simpleMessage.message == ImageTransferProtocol.FLOW_CONTROL_CONTINUE){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received FLOW_CONTROL_CONTINUE from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_READY to CAN_SEND.");
							threadState = thread_state_t.CAN_SEND;
						}else if(simpleMessage.message == ImageTransferProtocol.STREAM_CONTROL_END){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received STREAM_CONTROL_END from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_READY to END_STREAM.");
							threadState = thread_state_t.END_STREAM;
						}
					}
					break;

				case WAIT_FOR_ACK:
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Reading message from server. State is WAIT_FOR_ACK.");
					auxiliary = readMessage();

					if(!validateImageTransferProtocolMessage(auxiliary)){
						// If the message received is not valid then send an UNRECOGNIZED message to the server.
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Received an unrecognized protocol message. State WAIT_FOR_ACK.");
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending UNRECOGNIZED message to server.");
						sendUnrecognizedMessage();

					}else{
						// Else if the message passed the validity check then proceed to the next protocol state.
						simpleMessage = (ImageTransferProtocolMessage)auxiliary;
						if(simpleMessage.message == ImageTransferProtocol.ACK_SEND_NEXT){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received ACK_SEND_NEXT from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_ACK to CAN_SEND.");
							threadState = thread_state_t.CAN_SEND;
						}else if(simpleMessage.message == ImageTransferProtocol.ACK_WAIT){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received ACK_WAIT from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_ACK to WAIT_FOR_READY.");
							threadState = thread_state_t.WAIT_FOR_READY;
						}else if(simpleMessage.message == ImageTransferProtocol.STREAM_CONTROL_END){
							threadState = thread_state_t.END_STREAM;
						}
					}
					break;

				case CAN_SEND:
					// Get the image and it's parameters from the monitor.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Getting image data.");
					Rect imageSize = camMonitor.getImageParameters();
					image = camMonitor.getImageData();

					// Compress the image as Jpeg.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Compressing image.");
					YuvImage yuvImage = new YuvImage(image, ImageFormat.NV21, imageSize.width(), imageSize.height(), null);
					yuvImage.compressToJpeg(imageSize, 90, outputStream);

					// Prepare the message for sending.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Building message.");
					imageMessage = new ImageDataMessage();
					imageMessage.imageWidth = imageSize.width();
					imageMessage.imageHeight = imageSize.height();
					imageMessage.data = outputStream.toByteArray();

					// Send the message.
					try{
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending message.");
						writer.writeObject(imageMessage);
					}catch(IOException io){
						Logger.log_e(TAG, CLASS_NAME + ".run() :: Error sending image to the server: " + io.getMessage());
					}

					// Clean up stuff.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Cleaning.");
					yuvImage = null;
					image = null;
					outputStream.reset();
					imageMessage = null;
					imageSize = null;

					Logger.log_d(TAG, CLASS_NAME + ".run() :: Image data successfuly sent.");
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from CAN_SEND to WAIT_FOR_ACK.");
					threadState = thread_state_t.WAIT_FOR_ACK;
					break;

				case END_STREAM:
					// Simply disconnect from the server.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Ending video stream.");
					disconnect();
					done = true;
					break;
				}
			}
		}
		Logger.log_d(TAG, CLASS_NAME + ".run() :: Thread finish reached.");
	}

	private void connectToServer(){
		try{
			Logger.log_i(TAG, CLASS_NAME + ".connectToServer() :: Connecting to the server at " + serverIp);
			socket = new Socket(InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			writer = new ObjectOutputStream(socket.getOutputStream());
			reader = new ObjectInputStream(socket.getInputStream());
			Logger.log_i(TAG, CLASS_NAME + ".connectToServer() :: Connection successful.");
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".connectToServer() :: Connection failed with message: " + io.getMessage());
		}
	}

	public void disconnect(){
		if(socket != null && socket.isConnected()){
			try{
				Logger.log_d(TAG, CLASS_NAME + ".disconnect() :: Closing socket.");
				socket.close();
			}catch (IOException io) {
				Logger.log_e(TAG, CLASS_NAME + ".connectToServer() :: " + io.getMessage());
			}
		}
	}

	public synchronized void finish(){
		done = true;
		Logger.log_i(TAG, CLASS_NAME + ".finish() :: Finishing thread.");
	}

	private void checkPause(){
		synchronized (threadPauseMonitor){
			while(pause){
				Logger.log_d(TAG, CLASS_NAME + ".checkPause() :: Pause requested.");
				try{ threadPauseMonitor.wait(); }catch(InterruptedException ie){}
			}
		}
	}

	private Object readMessage(){
		Object auxiliary;

		// Read a message from the server stream.
		try{
			auxiliary = reader.readObject();

		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException when reading in WAIT_FOR_READY state.");
			auxiliary = null;
			return null;
		}catch(ClassNotFoundException cn){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: ClassNotFoundException when reading in WAIT_FOR_READY state.");
			auxiliary = null;
			return null;
		}

		return auxiliary;
	}

	private boolean validateImageTransferProtocolMessage(Object message){
		if(message != null && message instanceof ImageTransferProtocolMessage)
			return true;
		else
			return false;
	}

	private void sendUnrecognizedMessage(){
		ImageTransferProtocolMessage message = new ImageTransferProtocolMessage();
		message.message = ImageTransferProtocol.UNRECOGNIZED;

		try{
			writer.writeObject(message);
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException when writing UNRECOGNIZED in WAIT_FOR_READY state.");
		}
		Logger.log_d(TAG, CLASS_NAME + ".run() :: UNRECOGNIZED message sent.");
	}

	public synchronized void pauseThread(){
		pause = true;
		Logger.log_d(TAG, CLASS_NAME + ".pauseThread() :: Pausing thread.");
	}

	public synchronized void resumeThread(){
		Logger.log_d(TAG, CLASS_NAME + ".resumeThread() :: Resuming thread.");
		synchronized (threadPauseMonitor) {
			pause = false;
			threadPauseMonitor.notifyAll();
		}
	}

	public boolean isConnected(){
		if(socket != null && socket.isConnected())
			return true;
		else
			return false;
	}
}
