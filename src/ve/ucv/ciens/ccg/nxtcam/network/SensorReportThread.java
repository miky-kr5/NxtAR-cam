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

	public SensorReportThread(String serverIp){
		super("Sensor Report Thread");
		this.serverIp = serverIp;
		done = false;
	}

	@Override
	public void run(){
		while(!done){

		}
	}

	public void finish(){
		done = true;
	}

	public boolean connectToServer(){
		boolean connected;
		try{
			socket = new Socket(serverIp, ProjectConstants.SERVER_TCP_PORT_3);
			writer = new ObjectOutputStream(socket.getOutputStream());
			connected = true;
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".connectToServer() :: IOException caught: " + io.getMessage());
			connected = false;
		}
		return connected;
	}
}
