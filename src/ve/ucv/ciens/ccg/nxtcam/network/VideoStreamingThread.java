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
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ve.ucv.ciens.ccg.networkdata.VideoFrameDataMessage;
import ve.ucv.ciens.ccg.nxtcam.camera.CameraImageMonitor;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

public class VideoStreamingThread extends Thread{
	private final String TAG = "IM_THREAD";
	private final String CLASS_NAME = VideoStreamingThread.class.getSimpleName();

	//private enum ProtocolState_t {WAIT_FOR_ACK, WAIT_FOR_READY, CAN_SEND, END_STREAM};

	private boolean /*pause,*/ done;
	private Object threadPauseMonitor;
	private CameraImageMonitor camMonitor;
	private Socket socket;
	DatagramSocket udpSocket;
	/*private ObjectOutputStream writer;
	private ObjectInputStream reader;*/
	private String serverIp;
	//private ProtocolState_t protocolState;
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	public VideoStreamingThread(String serverIp){
		super("Video Streaming Thread");
		this.serverIp = serverIp;
		//pause = false;
		done = false;
		threadPauseMonitor = new Object();
		socket = null;
		//writer = null;
		//reader = null;
		camMonitor = CameraImageMonitor.getInstance();
		//protocolState = ProtocolState_t.WAIT_FOR_READY;
	}

	/*public void run(){
		byte[] image;
		Object tmpMessage;
		VideoStreamingControlMessage controlMessage;
		VideoFrameDataMessage dataMessage;
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		connectToServer();

		if(!socket.isConnected()){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Not connected to a server. Finishing thread.");
			return;
		}else{
			while(!done){
				// checkPause();
				switch(protocolState){
				case WAIT_FOR_READY:
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Reading message from server. State is WAIT_FOR_READY.");
					tmpMessage = readMessage();

					if(!validateImageTransferProtocolMessage(tmpMessage)){
						// If the message received is not valid then send an UNRECOGNIZED message to the server.
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Received an unrecognized protocol message. State WAIT_FOR_READY.");
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending UNRECOGNIZED message to server.");
						sendUnrecognizedMessage();

					}else{
						// Else if the message passed the validity check then proceed to the next protocol state.
						controlMessage = (VideoStreamingControlMessage)tmpMessage;
						if(controlMessage.message == VideoStreamingProtocol.FLOW_CONTROL_CONTINUE){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received FLOW_CONTROL_CONTINUE from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_READY to CAN_SEND.");
							protocolState = ProtocolState_t.CAN_SEND;
						}else if(controlMessage.message == VideoStreamingProtocol.STREAM_CONTROL_END){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received STREAM_CONTROL_END from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_READY to END_STREAM.");
							protocolState = ProtocolState_t.END_STREAM;
						}
					}
					break;

				case WAIT_FOR_ACK:
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Reading message from server. State is WAIT_FOR_ACK.");
					tmpMessage = readMessage();

					if(!validateImageTransferProtocolMessage(tmpMessage)){
						// If the message received is not valid then send an UNRECOGNIZED message to the server.
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Received an unrecognized protocol message. State WAIT_FOR_ACK.");
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending UNRECOGNIZED message to server.");
						sendUnrecognizedMessage();

					}else{
						// Else if the message passed the validity check then proceed to the next protocol state.
						controlMessage = (VideoStreamingControlMessage)tmpMessage;
						if(controlMessage.message == VideoStreamingProtocol.ACK_SEND_NEXT){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received ACK_SEND_NEXT from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_ACK to CAN_SEND.");
							protocolState = ProtocolState_t.CAN_SEND;
						}else if(controlMessage.message == VideoStreamingProtocol.ACK_WAIT){
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Received ACK_WAIT from the server.");
							Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from WAIT_FOR_ACK to WAIT_FOR_READY.");
							protocolState = ProtocolState_t.WAIT_FOR_READY;
						}else if(controlMessage.message == VideoStreamingProtocol.STREAM_CONTROL_END){
							protocolState = ProtocolState_t.END_STREAM;
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
					dataMessage = new VideoFrameDataMessage();
					dataMessage.imageWidth = imageSize.width();
					dataMessage.imageHeight = imageSize.height();
					dataMessage.data = outputStream.toByteArray();

					// Send the message.
					try{
						Logger.log_d(TAG, CLASS_NAME + ".run() :: Sending message.");
						writer.writeObject(dataMessage);
					}catch(IOException io){
						Logger.log_e(TAG, CLASS_NAME + ".run() :: Error sending image to the server: " + io.getMessage());
					}

					// Clean up stuff.
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Cleaning.");
					yuvImage = null;
					image = null;
					outputStream.reset();
					dataMessage = null;
					imageSize = null;

					Logger.log_d(TAG, CLASS_NAME + ".run() :: Image data successfuly sent.");
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Transitioning from CAN_SEND to WAIT_FOR_ACK.");
					protocolState = ProtocolState_t.WAIT_FOR_ACK;
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
	}*/

	public void run(){
		//connectToServer();

		try{
			udpSocket = new DatagramSocket();
			//udpSocket.setSendBufferSize(Integer.MAX_VALUE);
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException received creating socket " + io.getMessage());
			System.exit(1);
		}

		/*if(!socket.isConnected()){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Not connected to a server. Finishing thread.");
			return;

		}else{*/
			while(!done){
				//sendImage();
				sendUdp();
				try{
					sleep(50L);
				}catch(InterruptedException ie){}
			}
		//}

		Logger.log_d(TAG, CLASS_NAME + ".run() :: Thread finish reached.");
	}

	private byte[] int2ByteArray(int integer){
		int shift;
		byte[] array = new byte[4];
		for(int i = 0; i < 4; i++){
			shift = i << 3;
			array[3 - i] = (byte)((integer & (0xff << shift)) >>> shift);
		}
		return array;
	}

	private void sendUdp(){
		int bufferSize;
		byte[] image;
		byte[] buffer;
		byte[] size;
		DatagramPacket packet;
		VideoFrameDataMessage message;
		Rect imageSize;
		YuvImage yuvImage;

		image = camMonitor.getImageData();
		imageSize = camMonitor.getImageParameters();

		yuvImage = new YuvImage(image, ImageFormat.NV21, imageSize.width(), imageSize.height(), null);
		yuvImage.compressToJpeg(imageSize, 90, outputStream);

		message = new VideoFrameDataMessage();
		message.data = outputStream.toByteArray();
		message.imageWidth = imageSize.width();
		message.imageHeight = imageSize.height();

		outputStream.reset();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try{
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(message);
			oos.flush();
			oos.reset();
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".sendUdp() :: IOException received while serializing." + io.getMessage());
			return;
		}

		buffer = baos.toByteArray();
		baos.reset();
		bufferSize = buffer.length;
		size = int2ByteArray(bufferSize);

		try{
			packet = new DatagramPacket(size, 4, InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			udpSocket.send(packet);

			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			udpSocket.send(packet);

		}catch(UnknownHostException uo){
			Logger.log_e(TAG, CLASS_NAME + ".sendUdp() :: UnknownHostException received " + uo.getMessage());
			return;
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".sendUdp() :: IOException buffer size is " + Integer.toString(buffer.length));
			Logger.log_e(TAG, CLASS_NAME + ".sendUdp() :: IOException received while sending " + io.getMessage());
			return;
		}
	}

	/*private void sendImage(){
		byte[] image;
		YuvImage yuvImage;
		VideoFrameDataMessage message;
		Rect imageSize;

		image = camMonitor.getImageData();
		if(image == null){
			Logger.log_e(TAG, CLASS_NAME + ".sendImage() :: image is null, skipping frame.");
			return;
		}
		imageSize = camMonitor.getImageParameters();

		// Compress the image as Jpeg.
		Logger.log_d(TAG, CLASS_NAME + ".sendImage() :: Compressing image.");
		yuvImage = new YuvImage(image, ImageFormat.NV21, imageSize.width(), imageSize.height(), null);
		yuvImage.compressToJpeg(imageSize, 90, outputStream);

		Logger.log_d(TAG, CLASS_NAME + ".sendImage() :: Building message.");
		message = new VideoFrameDataMessage();
		message.data = outputStream.toByteArray();
		message.imageWidth = imageSize.width();
		message.imageHeight = imageSize.height();

		try{
			Logger.log_d(TAG, CLASS_NAME + ".sendImage() :: Sending message.");
			writer.writeObject(message);
			writer.flush();
			writer.reset();
			Logger.log_d(TAG, CLASS_NAME + ".sendImage() :: Message sent successfully: ");
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".sendImage() :: Error sending image to the server: " + io.getMessage());

		}finally{
			Logger.log_d(TAG, CLASS_NAME + ".sendImage() :: Cleaning.");
			outputStream.reset();
			image = null;
			yuvImage = null;
			message = null;
			imageSize = null;
			System.gc();
		}
	}*/

	private void connectToServer(){
		try{
			Logger.log_i(TAG, CLASS_NAME + ".connectToServer() :: Connecting to the server at " + serverIp);
			socket = new Socket(InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			/*writer = new ObjectOutputStream(socket.getOutputStream());
			reader = new ObjectInputStream(socket.getInputStream());*/
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

	/*private void checkPause(){
		synchronized (threadPauseMonitor){
			while(pause){
				Logger.log_d(TAG, CLASS_NAME + ".checkPause() :: Pause requested.");
				try{ threadPauseMonitor.wait(); }catch(InterruptedException ie){}
			}
		}
	}

	private Object readMessage(){
		Object tmpMessage;

		// Read a message from the server stream.
		try{
			tmpMessage = reader.readObject();

		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException when reading in WAIT_FOR_READY state.");
			tmpMessage = null;
			return null;
		}catch(ClassNotFoundException cn){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: ClassNotFoundException when reading in WAIT_FOR_READY state.");
			tmpMessage = null;
			return null;
		}

		return tmpMessage;
	}

	private boolean validateImageTransferProtocolMessage(Object message){
		if(message != null && message instanceof VideoStreamingControlMessage)
			return true;
		else
			return false;
	}

	private void sendUnrecognizedMessage(){
		VideoStreamingControlMessage message = new VideoStreamingControlMessage();
		message.message = VideoStreamingProtocol.UNRECOGNIZED;

		try{
			writer.writeObject(message);
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException when writing UNRECOGNIZED in WAIT_FOR_READY state.");
		}
		Logger.log_d(TAG, CLASS_NAME + ".run() :: UNRECOGNIZED message sent.");
	}*/

	public synchronized void pauseThread(){
		//pause = true;
		Logger.log_d(TAG, CLASS_NAME + ".pauseThread() :: Pausing thread.");
	}

	public synchronized void resumeThread(){
		Logger.log_d(TAG, CLASS_NAME + ".resumeThread() :: Resuming thread.");
		synchronized (threadPauseMonitor) {
			//pause = false;
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
