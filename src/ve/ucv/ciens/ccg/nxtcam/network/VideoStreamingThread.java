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

	private boolean pause, done;
	private Object threadPauseMonitor;
	private CameraImageMonitor camMonitor;
	private Socket socket;
	DatagramSocket udpSocket;
	private String serverIp;
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	public VideoStreamingThread(String serverIp){
		super("Video Streaming Thread");
		this.serverIp = serverIp;
		done = false;
		pause = false;
		threadPauseMonitor = new Object();
		socket = null;
		camMonitor = CameraImageMonitor.getInstance();
	}

	public void run(){

		try{
			udpSocket = new DatagramSocket();

		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException received creating socket " + io.getMessage());
			System.exit(1);
		}


		while(!done){

			synchronized (threadPauseMonitor) {
				while(pause){
					try{ threadPauseMonitor.wait(); }catch(InterruptedException ie){ };
				}
			}

			sendUdp();
			try{
				sleep(50L);
			}catch(InterruptedException ie){}
		}


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
			packet = new DatagramPacket(size, 4, InetAddress.getByName(serverIp), ProjectConstants.VIDEO_STREAMING_PORT);
			udpSocket.send(packet);

			packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverIp), ProjectConstants.VIDEO_STREAMING_PORT);
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

	public void pauseThread(){
		synchronized (threadPauseMonitor) {
			pause = true;
		}
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
