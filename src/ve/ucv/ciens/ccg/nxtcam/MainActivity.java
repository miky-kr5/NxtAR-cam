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
package ve.ucv.ciens.ccg.nxtcam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;

import ve.ucv.ciens.ccg.nxtcam.dialogs.ConnectRobotDialog;
import ve.ucv.ciens.ccg.nxtcam.dialogs.ConnectRobotDialog.ConnectRobotDialogListener;
import ve.ucv.ciens.ccg.nxtcam.dialogs.WifiOnDialog;
import ve.ucv.ciens.ccg.nxtcam.dialogs.WifiOnDialog.WifiOnDialogListener;
import ve.ucv.ciens.ccg.nxtcam.network.BTCommunicator;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Entry point por the NxtCAM application.
 * 
 * This activity shows a splashscreen and handles the search for the controller device
 * via a simle ad hoc UDP based service discovery method. Basically, it just listens for
 * multicast packets sent by the controller device on the multicast address defined in
 * ProjectConstants.java. When the packet is received the next activity of the application
 * is launched with the ip address found. The service discovery process continues until a
 * datagram carrying the string "NxtAR server here!" is received.
 * 
 * @author miky
 */
public class MainActivity extends Activity implements WifiOnDialogListener, ConnectRobotDialogListener{
	// Cosntant fields.
	private final String TAG = "NXTCAM_MAIN";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_CAM_ACTIVITY = 2;

	// Gui components
	private Button startButton;
	private Button connectButton;
	private ProgressDialog progressDialog;

	// Resources.
	private BTCommunicator btManager;
	private WifiManager wifiManager;

	// Variables.
	private boolean wifiOnByMe;
	private boolean btOnByMe;
	private boolean changingActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up fields.
		wifiOnByMe = false;
		btOnByMe = false;
		changingActivity = false;

		// Set up gui components.
		startButton = (Button)findViewById(R.id.startButton);
		startButton.setEnabled(false);
		connectButton = (Button)findViewById(R.id.connectButton);

		// Set up services.
		btManager = BTCommunicator.getInstance();
		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

		if(!btManager.isBTSupported()){
			// El dispositivo no soporta BlueTooth.
			Toast.makeText(this, R.string.bt_no_support, Toast.LENGTH_LONG).show();
			finish();
			System.exit(0);
		}
	}

	@Override
	public void onResume(){
		super.onResume();

		if(!btManager.isBTEnabled()){
			enableBT();
		}else if(btManager.isBTEnabled() && !wifiManager.isWifiEnabled()){
			enableWifi();
		}
	}

	@Override
	public void onPause(){
		super.onPause();

		if(!changingActivity){
			if(btManager.isBTEnabled() && btOnByMe)
				btManager.disableBT();
			if(wifiManager.isWifiEnabled() && wifiOnByMe)
				setWifi(false);
		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(btManager.isConnected()){
			try{
				btManager.stopConnection();
			}catch(IOException io){
				Logger.log_e(TAG, CLASS_NAME + ".onDestroy() :: Error closing the connection with the robot: " + io.getMessage());
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onActivityResult(int request, int result, Intent data){
		if(request == REQUEST_ENABLE_BT){
			if(result == RESULT_OK){
				if(!wifiManager.isWifiEnabled())
					enableWifi();
			}else{
				Toast.makeText(this, R.string.bt_on_fail, Toast.LENGTH_SHORT).show();
			}
		}else if(request == REQUEST_CAM_ACTIVITY){
			changingActivity = false;
			if(result == ProjectConstants.RESULT_CAMERA_FAILURE){
				Toast.makeText(this, R.string.cam_fail, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Start the camera capture activity if a server was found through service discovery.
	 * 
	 * @param serverFound Indicates if a server was found, doh!
	 * @param ipAddress The ip address of the server.
	 */
	private void startCamActivity(boolean serverFound, String ipAddress){
		if(serverFound){
			Logger.log_d(TAG, CLASS_NAME + ".startCamActivity() :: Launching camera activity.");
			Intent intent = new Intent(this, CamActivity.class);
			changingActivity = true;
			intent.putExtra("address", ipAddress);
			startActivityForResult(intent, REQUEST_CAM_ACTIVITY);
		}else{
			Logger.log_d(TAG, CLASS_NAME + ".startCamActivity() :: Cannot launch camera activity.");
			Toast.makeText(this, R.string.badIpToast, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Sets the state of the device's WiFi radio.
	 * 
	 * @param radioState The state to set the radio to; true for on, false for off.
	 */
	private void setWifi(boolean radioState){
		wifiManager.setWifiEnabled(radioState);
		Logger.log_d(TAG, CLASS_NAME + ".setWifi() :: setting wifi to " + (radioState ? "on" : "off"));
		if(radioState)
			wifiOnByMe = true;
		else
			wifiOnByMe = false;
	}

	/**
	 * Shows a WifiOnDialog.
	 */
	private void enableWifi(){
		if(!wifiManager.isWifiEnabled()){
			DialogFragment wifiOn = new WifiOnDialog();
			((WifiOnDialog)wifiOn).setWifiManager(wifiManager);
			wifiOn.show(getFragmentManager(), "wifi_on");
		}
	}

	/**
	 * Launches the standard Bluetooth enable activity.
	 */
	private void enableBT(){
		Logger.log_d(TAG, CLASS_NAME + ".enableBT() :: Enabling the Bluetooth radio.");
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		btOnByMe = true;
	}

	/**
	 * Commodity method for showing toasts from an AsyncTask.
	 * 
	 * @param stringId The id of the string resource to show on the toast.
	 * @param length Time to show the toast.
	 */
	protected void showToast(int stringId, int length){
		Toast.makeText(this, stringId, length).show();
	}

	/**
	 * Commodity method that builds an standard Android progress dialog.
	 * 
	 * The dialog is created as not cancellable and uses an undeterminate spinner as visual style.
	 * 
	 * @param msg The descriptive text shown by the dialog.
	 * @return The built dialog.
	 */
	private ProgressDialog buildProgressDialog(String msg){
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(msg);
		dialog.setCancelable(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setProgress(0);
		return dialog;
	}

	/**
	 * Listener method for the WifiOnDialog.
	 * 
	 * This method is called when the user chooses to accept the dialog. It just shows an information
	 * message with a toast and marks the WiFi radio as turned on by the application.
	 * 
	 * @param dialog The dialog that called this method. 
	 */
	@Override
	public void onWifiOnDialogPositiveClick(DialogFragment dialog) {
		Toast.makeText(this, R.string.wifi_on_success, Toast.LENGTH_SHORT).show();
		wifiOnByMe = true;
	}

	/**
	 * Listener method for the WifiOnDialog.
	 * 
	 * This method is called when the user chooses to cancel the dialog. It just shows an error message
	 * with a toast and finishes the application.
	 * 
	 * @param dialog The dialog that called this method. 
	 */
	@Override
	public void onWifiOnDialogNegativeClick(DialogFragment dialog) {
		Toast.makeText(this, R.string.wifi_on_fail, Toast.LENGTH_LONG).show();
		finish();
	};

	/**
	 * Shows the robot selection dialog.
	 * 
	 * @param view The view that called this method.
	 */
	public void connectWithRobot(View view){
		if(btManager.isBTEnabled()){
			DialogFragment connectBot = new ConnectRobotDialog();
			connectBot.show(getFragmentManager(), "connect_bot");
		}
	}

	/**
	 * Launches the service discovery task.
	 */
	public void startConnections(View view){
		ServiceDiscoveryTask serviceDiscovery = new ServiceDiscoveryTask();
		serviceDiscovery.execute();
	}

	/**
	 * Listener method for the ConnectRobotDialog.
	 * 
	 * When a user selects a robot to connect to in the dialog, this method launches the connection setup task
	 * defined in the ConnectRobotTask.
	 * 
	 * @param dialog The dialog that called this method.
	 * @param robot The robot selected by the user in the format NAME\nMAC_ADDRESS
	 */
	@Override
	public void onConnectRobotDialogItemClick(DialogFragment dialog, String robot) {
		String macAddress = robot.substring(robot.indexOf('\n')+1);
		Logger.log_d(TAG, CLASS_NAME + ".onConnectRobotDialogItemClick() :: MAC address: " + macAddress);
		connectButton.setEnabled(false);
		ConnectRobotTask robotTask = new ConnectRobotTask(macAddress);
		robotTask.execute();
	}

	/**
	 * Asynchronous task for ad hoc UDP service discovery.
	 * 
	 * @author Miguel Angel Astor Romero
	 */
	private class ServiceDiscoveryTask extends AsyncTask<Void, Void, Boolean>{

		private final String CLASS_NAME = ServiceDiscoveryTask.class.getSimpleName();

		private MulticastSocket udpSocket;
		private DatagramPacket packet;
		private MulticastLock multicastLock;

		public ServiceDiscoveryTask(){
			// Open a multicast socket and join the project's multicast group.
			try{
				udpSocket = new MulticastSocket(ProjectConstants.SERVER_UDP_PORT);
				InetAddress group = InetAddress.getByName(ProjectConstants.MULTICAST_ADDRESS);
				udpSocket.joinGroup(group);
			}catch(IOException io){
				Logger.log_e(TAG ,CLASS_NAME + ".ServiceDiscoveryTask() :: " + io.getMessage());
			}
		}

		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			progressDialog = buildProgressDialog(getString(R.string.serv_wait));
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params){
			boolean result, done = false;
			byte[] buffer = ProjectConstants.SERVICE_DISCOVERY_MESSAGE.getBytes();

			// Create a buffer and tell Android we want to receive multicast datagrams.
			packet = new DatagramPacket(buffer, buffer.length);
			multicastLock = wifiManager.createMulticastLock(TAG);
			multicastLock.setReferenceCounted(true);
			multicastLock.acquire();

			// Listen for a UDP datagram on the multicast group.
			// If the datagram received contains a string with it's content equal to "NxtAR server here!"
			// then assume the server found is a valid controller device.
			try{
				while(!done){
					udpSocket.receive(packet);
					Logger.log_d(TAG, CLASS_NAME + ".run() :: Found a server at " + packet.getAddress().getHostAddress());
					String received = new String(packet.getData());
					Logger.log_d(TAG, CLASS_NAME + ".doInBackground() :: Packet payload is\n" + received);
					if(received.compareTo(ProjectConstants.SERVICE_DISCOVERY_MESSAGE) == 0)
						done = true;
				}
				result = true;
			}catch(IOException io){
				Logger.log_e(TAG, CLASS_NAME + ".doInBackground() :: " + io.getMessage());
				result = false;
			}

			// Tell Android we do not want to receive more UDP datagrams to save battery life.
			if(multicastLock != null){
				multicastLock.release();
				multicastLock = null;
			}

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);

			progressDialog.dismiss();
			progressDialog = null;

			// If a server was found then start the next activity.
			startButton.setEnabled(false);

			if(packet != null){
				showToast(R.string.serv_connected, Toast.LENGTH_SHORT);
				startCamActivity(result, packet.getAddress().getHostAddress());
			}else{
				showToast(R.string.serv_fail, Toast.LENGTH_SHORT);
				startCamActivity(false, null);
			}
		}
	}

	/**
	 * This task handles the establishing of the connection with the NXT robot.
	 * 
	 * @author miky
	 */
	private class ConnectRobotTask extends AsyncTask<Void, Void, Boolean>{
		private final String CLASS_NAME = ConnectRobotTask.class.getSimpleName();
		private String macAddress;

		public ConnectRobotTask(String macAddress){
			this.macAddress = macAddress;
		}

		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			progressDialog = buildProgressDialog(getString(R.string.bt_wait));
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params){
			boolean connSet;
			Logger.log_d(TAG, CLASS_NAME + "doInBackground() :: Establishing connection with the robot.");
			try{
				connSet = btManager.establishConnection(macAddress);
			}catch(IOException e){
				Logger.log_e(TAG, CLASS_NAME + "doInBackground() :: Error during the connection attempt.");
				connSet = false;
			}
			return connSet;
		}

		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);

			progressDialog.dismiss();
			progressDialog = null;

			if(result){
				Logger.log_d(TAG, CLASS_NAME + "doInBackground() :: Connection successful.");
				showToast(R.string.conn_established, Toast.LENGTH_SHORT);
				startButton.setEnabled(true);
			}else{
				Logger.log_d(TAG, CLASS_NAME + "doInBackground() :: Connection failed.");
				showToast(R.string.conn_failed, Toast.LENGTH_LONG);
				connectButton.setEnabled(true);
			}
		}
	}
}
