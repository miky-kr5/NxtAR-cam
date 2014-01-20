package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import ve.ucv.ciens.ccg.networkdata.MotorEvent;
import ve.ucv.ciens.ccg.nxtcam.robotcontrol.MotorEventQueue;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;

public class MotorControlThread extends Thread {
	private static final String TAG = "MOTOR_CONTROL";
	private static final String CLASS_NAME = MotorControlThread.class.getSimpleName();

	private Socket socket;
	private String serverIp;
	private MotorEventQueue queue;
	private boolean done;
	private ObjectInputStream reader;
	private boolean connected;

	public MotorControlThread(String serverIp){
		super("Motor Control Thread");
		this.serverIp = serverIp;
		done = false;
		connected = false;
		queue = MotorEventQueue.getInstance();
	}

	@Override
	public void run(){
		if(!connected){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: The thread is not connected to a server. Finishing.");
			return;
		}else{
			while(!done){

			}
		}
	}

	public void finish(){
		done = true;
	}

	public boolean connectToServer(){
		try{
			socket = new Socket(serverIp, ProjectConstants.SERVER_TCP_PORT_3);
			reader = new ObjectInputStream(socket.getInputStream());
			connected = true;
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".connectToServer() :: IOException caught: " + io.getMessage());
			connected = false;
		}
		return connected;
	}

	private Object readMessage(){
		Object message;
		try{
			message = reader.readObject();
		}catch(ClassNotFoundException cn){
			Logger.log_e(TAG, CLASS_NAME + ".readMessage() :: ClassNotFoundException caught: " + cn.getMessage());
			message = null;
		}catch(IOException io){
			Logger.log_e(TAG, CLASS_NAME + ".readMessage() :: IOException caught: " + io.getMessage());
			message = null;
		}
		return message;
	}

	private MotorEvent verifyMessage(Object message){
		if(message != null && message instanceof MotorEvent){
			return (MotorEvent)message;
		}else{
			return null;
		}
	}
}
