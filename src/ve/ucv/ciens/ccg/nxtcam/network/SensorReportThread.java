package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;

public class SensorReportThread extends Thread{
	private static final String TAG = "SENSOR_REPORT";
	private static final String CLASS_NAME = SensorReportThread.class.getSimpleName();

	private Socket socket;
	private String serverIp;
	private boolean done;
	private ObjectOutputStream writer;
	private boolean connected;

	public SensorReportThread(String serverIp){
		super("Sensor Report Thread");
		this.serverIp = serverIp;
		done = false;
		connected = false;
	}

	@Override
	public void run(){
		if(connectToServer()){
			while(!done){

			}
		}else{
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Could not connect to the server.");
		}
	}

	public void finish(){
		done = true;
	}

	private boolean connectToServer(){
		boolean connected;
		try{
			socket = new Socket(serverIp, ProjectConstants.SENSOR_REPORT_PORT);
			writer = new ObjectOutputStream(socket.getOutputStream());
			connected = true;
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".connectToServer() :: IOException caught: " + io.getMessage());
			connected = false;
		}
		return connected;
	}

	public boolean isConnected(){
		return connected;
	}
}
