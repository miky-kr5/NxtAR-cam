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

import java.io.IOException;

import ve.ucv.ciens.ccg.networkdata.MotorEvent;
import ve.ucv.ciens.ccg.networkdata.MotorEvent.motor_t;
import ve.ucv.ciens.ccg.nxtcam.network.protocols.MotorMasks;
import ve.ucv.ciens.ccg.nxtcam.robotcontrol.MotorEventQueue;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;

public class NxtBTCommThread extends Thread{
	private static final String TAG = "LCP_THREAD";
	private static final String CLASS_NAME = NxtBTCommThread.class.getSimpleName();

	private boolean done;
	private BTCommunicator btComm;
	private MotorControlThread motorControl;

	private MotorEventQueue queue;

	public NxtBTCommThread(String serverIp){
		super("Robot Control Main Thread");
		btComm = BTCommunicator.getInstance();
		done = false;
		motorControl = new MotorControlThread(serverIp);
		queue = MotorEventQueue.getInstance();
	}

	public void run(){
		long then, now, delta;
		MotorEvent event;
		byte[] msg = new byte[2];

		motorControl.start();

		then = System.currentTimeMillis();

		while(!motorControl.isConnected()){
			now = System.currentTimeMillis();
			delta = now - then;
			if(delta > 9000L){
				Logger.log_e(TAG, CLASS_NAME + ".run() :: Thread motorControl could not connect to the server.");
				return;
			}
		}

		while(!done){
			if(btComm.isBTEnabled() && btComm.isConnected()){
				msg[0] = 0x00;
				msg[1] = 0x00;

				event = queue.getNextEvent();

				try{
					// Set the motor bit.
					msg[0] |= (event.getMotor() == motor_t.MOTOR_A) ? MotorMasks.MOTOR_A : 0;
					msg[0] |= (event.getMotor() == motor_t.MOTOR_B) ? MotorMasks.MOTOR_B : 0;
					msg[0] |= (event.getMotor() == motor_t.MOTOR_C) ? MotorMasks.MOTOR_C : 0;
					// Set the direction bit.
					if(event.getPower() > 0) msg[0] |= MotorMasks.DIRECTION;
					// TODO: Set the recenter bits.

					// Set the power byte.
					msg[1] = (byte)Math.abs(event.getPower());

					// Send the message.
					btComm.writeMessage(msg);
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Message sent to the robot.");

					try{ sleep(40); }catch(InterruptedException ie){ }

				}catch(IOException io){
					Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException sending message to the robot: " + io.getMessage());
				}
			}else{
				Logger.log_e(TAG, CLASS_NAME +  ".run() :: The robot disconnected or was never available.");
				break;
			}
		}
	}

	public void finish(){
		done = true;
	}
}
