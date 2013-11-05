package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.camera.CameraImageMonitor;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.util.Log;

public class ImageTransferThread extends Thread{

	private final String TAG = "IM_THREAD";
	private final String CLASS_NAME = ImageTransferThread.class.getSimpleName();

	private boolean pause, done, connected;
	private Object threadPauseMonitor;
	private CameraImageMonitor camMonitor;
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private byte[] image;

	public ImageTransferThread(){
		pause = false;
		done = false;
		connected = false;
		threadPauseMonitor = new Object();
		socket = null;
		writer = null;
		reader = null;
		camMonitor = CameraImageMonitor.getInstance();
	}

	public void run(){
		if(!connected){
			Log.e(TAG, CLASS_NAME + ".run() :: Not connected to a server. Finishing thread.");
		}else{
			while(!done){
				checkPause();
				image = camMonitor.getImageData();
				// TODO: implement image transfer protocol.
			}
		}
	}

	public void connectToServer(String serverIp){
		try{
			if(ProjectConstants.DEBUG) Log.i(TAG, CLASS_NAME + ".connectToServer() :: Connecting to the server at " + serverIp);
			socket = new Socket(InetAddress.getByName(serverIp), ProjectConstants.SERVER_TCP_PORT_1);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			connected = true;
			if(ProjectConstants.DEBUG) Log.i(TAG, CLASS_NAME + ".connectToServer() :: Connection successful.");
		}catch(IOException io){
			Log.e(TAG, CLASS_NAME + ".connectToServer() :: Connection failed with message: " + io.getMessage());
			connected = false;
		}
	}

	public synchronized void finish(){
		done = true;
		if(ProjectConstants.DEBUG) Log.i(TAG, CLASS_NAME + ".finish() :: Finishing thread.");
	}

	private void checkPause(){
		synchronized (threadPauseMonitor){
			while(pause){
				if(ProjectConstants.DEBUG) Log.d(TAG, CLASS_NAME + ".checkPause() :: Pause requested.");
				try{ threadPauseMonitor.wait(); }catch(InterruptedException ie){}
			}
		}
	}

	public synchronized void pauseThread(){
		pause = true;
		if(ProjectConstants.DEBUG) Log.d(TAG, CLASS_NAME + ".pauseThread() :: Pausing thread.");
	}

	public synchronized void resumeThread(){
		if(ProjectConstants.DEBUG) Log.d(TAG, CLASS_NAME + ".resumeThread() :: Resuming thread.");
		synchronized (threadPauseMonitor) {
			pause = false;
			threadPauseMonitor.notifyAll();
		}
	}

	public boolean isConnected(){
		return connected;
	}
}
