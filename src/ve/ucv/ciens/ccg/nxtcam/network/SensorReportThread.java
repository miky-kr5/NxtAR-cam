package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;

public class SensorReportThread extends Thread{
	private static final String TAG = "SENSOR_REPORT";
	private static final String CLASS_NAME = SensorReportThread.class.getSimpleName();

	private Socket socket;
	private String serverIp;
	private boolean done;
	private OutputStream writer;
	private boolean connected;
	private BTCommunicator btComm;

	public SensorReportThread(String serverIp){
		super("Sensor Report Thread");
		this.serverIp = serverIp;
		done = false;
		connected = false;
		btComm = BTCommunicator.getInstance();
	}

	@Override
	public void run(){
		byte[] lightReading;

		if(connectToServer()){
			while(!done){
				if(btComm.isBTEnabled() && btComm.isConnected()){
					try{
						lightReading = btComm.readMessage(1);
						writer.write(lightReading);
					}catch(IOException io){
						Logger.log_e(TAG, CLASS_NAME + "run(): IOException: " + io.getMessage());
						done = true;
					}
				}else{
					Logger.log_e(TAG, CLASS_NAME +  ".run() :: The robot disconnected or was never available.");
					break;
				}
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
			writer = socket.getOutputStream();
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
