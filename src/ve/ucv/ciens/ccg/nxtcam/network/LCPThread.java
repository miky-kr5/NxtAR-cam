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

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;

public class LCPThread extends Thread{
	private static final String TAG = "LCP_THREAD";
	private static final String CLASS_NAME = LCPThread.class.getSimpleName();

	private boolean done;
	private boolean reportSensors;
	private BTCommunicator btComm;
	private MotorControlThread motorControl;
	private SensorReportThread sensorReport;
	
	public LCPThread(String serverIp){
		super("Robot Control Main Thread");
		btComm = BTCommunicator.getInstance();
		done = false;
		motorControl = new MotorControlThread(serverIp);
		sensorReport = new SensorReportThread(serverIp);
	}
	
	public void run(){
		if(!motorControl.connectToServer()){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Thread motorControl could not connect to the server.");
			return;
		}
		if(!(reportSensors = sensorReport.connectToServer())){
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Thread sensorReport could not connect to the server.");
			Logger.log_e(TAG, CLASS_NAME + ".run() :: Sensor data will not be reported to server app.");
		}
		
		while(!done){
			if(btComm.isBTEnabled() && btComm.isConnected()){
				Logger.log_d(TAG, CLASS_NAME + ".run() :: Connected.");
				if(reportSensors)
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Sensor data can be reported.");
			}
		}
	}
	
	public void finish(){
		done = true;
	}
}
