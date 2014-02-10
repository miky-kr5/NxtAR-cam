package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import ve.ucv.ciens.ccg.networkdata.MotorEvent;
import ve.ucv.ciens.ccg.networkdata.MotorEvent.motor_t;
import ve.ucv.ciens.ccg.networkdata.MotorEventACK;
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
	private ObjectOutputStream writer;
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
		Object msg;
		MotorEvent event;
		MotorEventACK ack;

		if(!connectToServer()){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: The thread is not connected to a server. Finishing.");
			return;
		}else{
			while(!done){
				// Receive a message and enqueue it;
				msg = readMessage();
				event = verifyMessage(msg);
				if(event != null){
					queue.addEvent(event);
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Motor control message enqueued.");
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Motor ID: " + (event.getMotor() == motor_t.MOTOR_A ? "MOTOR_A" : "MOTOR_C"));
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Motor power: " + Byte.toString(event.getPower()));
				}else{
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Message could not be verified;");
				}

				// Send corresponding ack;
				ack = new MotorEventACK(queue.getSize() >= 10);
				try{
					writer.writeObject(ack);
					Logger.log_i(TAG, CLASS_NAME + ".run() :: First ACK sent.");
				}catch(Exception ex){
					Logger.log_e(TAG, CLASS_NAME + ".run() :: Exception while sending first ACK: " + ex.getMessage());
					break;
				}

				if(ack.isClientQueueFull()){
					while(queue.getSize() >= 10){ }

					ack = new MotorEventACK(false);

					try{
						writer.writeObject(ack);
					}catch(Exception ex){
						Logger.log_i(TAG, CLASS_NAME + ".run() :: Second ACK sent.");
						Logger.log_e(TAG, CLASS_NAME + ".run() :: Exception while sending second ACK: " + ex.getMessage());
						break;
					}
				}

				event = null;
				ack = null;
				msg = null;
			}
			try{
				socket.close();
			}catch(IOException io){
				Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException while closing socket: " + io.getMessage());
			}
		}
	}

	public void finish(){
		done = true;
	}

	public boolean connectToServer(){
		try{
			socket = new Socket(serverIp, ProjectConstants.MOTOR_CONTROL_PORT);
			reader = new ObjectInputStream(socket.getInputStream());
			writer = new ObjectOutputStream(socket.getOutputStream());
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
			Logger.log_i(TAG, CLASS_NAME + ".readMessage() :: Motor control message received.");
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
			Logger.log_i(TAG, CLASS_NAME + ".verifyMessage() :: Valid motor control message received.");
			return (MotorEvent)message;
		}else{
			return null;
		}
	}
}
