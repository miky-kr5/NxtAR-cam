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
import ve.ucv.ciens.ccg.nxtcam.network.protocols.LegoCommunicationProtocol;
import ve.ucv.ciens.ccg.nxtcam.robotcontrol.MotorEventQueue;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;

public class LCPThread extends Thread{
	private static final String TAG = "LCP_THREAD";
	private static final String CLASS_NAME = LCPThread.class.getSimpleName();

	private boolean done;
	private boolean reportSensors;
	private BTCommunicator btComm;
	private MotorControlThread motorControl;
	private SensorReportThread sensorReport;

	private MotorEventQueue queue;

	public LCPThread(String serverIp){
		super("Robot Control Main Thread");
		btComm = BTCommunicator.getInstance();
		done = false;
		motorControl = new MotorControlThread(serverIp);
		sensorReport = new SensorReportThread(serverIp);
		queue = MotorEventQueue.getInstance();
	}

	public void run(){
		long then, now, delta;
		MotorEvent event;

		//sensorReport.start();
		motorControl.start();

		then = System.currentTimeMillis();

		while(!motorControl.isConnected()){
			now = System.currentTimeMillis();
			delta = now - then;
			if(delta > 5000L){
				Logger.log_e(TAG, CLASS_NAME + ".run() :: Thread motorControl could not connect to the server.");
				return;
			}
		}
		/*if(!(reportSensors = sensorReport.connectToServer())){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Thread sensorReport could not connect to the server.");
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Sensor data will not be reported to server app.");
		}*/

		while(!done){
			if(btComm.isBTEnabled() && btComm.isConnected()){
				//Logger.log_d(TAG, CLASS_NAME + ".run() :: Connected.");

				event = queue.getNextEvent();

				try{
					btComm.writeMessage(
							LegoCommunicationProtocol.setOutputState(
									event.getMotor() == motor_t.MOTOR_A ? LegoCommunicationProtocol.PORT_0 : LegoCommunicationProtocol.PORT_2,
											event.getPower())
							);
					Logger.log_i(TAG, CLASS_NAME + ".run() :: Message sent to the robot.");

				}catch(IOException io){
					Logger.log_e(TAG, CLASS_NAME + ".run() :: IOException sending message to the robot.");
				}

				/*if(reportSensors){
					//Logger.log_d(TAG, CLASS_NAME + ".run() :: Sensor data can be reported.");
				}*/
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
