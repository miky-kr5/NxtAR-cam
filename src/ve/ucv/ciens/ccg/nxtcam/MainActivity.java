package ve.ucv.ciens.ccg.nxtcam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import ve.ucv.ciens.ccg.nxtcam.dialogs.ConnectRobotDialog;
import ve.ucv.ciens.ccg.nxtcam.dialogs.ConnectRobotDialog.ConnectRobotDialogListener;
import ve.ucv.ciens.ccg.nxtcam.dialogs.WifiOnDialog;
import ve.ucv.ciens.ccg.nxtcam.dialogs.WifiOnDialog.WifiOnDialogListener;
import ve.ucv.ciens.ccg.nxtcam.network.BTCommunicator;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.app.Activity;
import android.app.DialogFragment;
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
 *
 */
public class MainActivity extends Activity implements WifiOnDialogListener, ConnectRobotDialogListener{
	// Cosntant fields.
	private final String TAG = "NXTCAM_MAIN";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();
	private static final int REQUEST_ENABLE_BT = 1;

	// Gui components
	private Button startButton;
	private Button connectButton;

	// Resources.
	private BTCommunicator btManager;
	private WifiManager wifiManager;

	// Variables.
	private boolean wifiOnByMe;
	private boolean btOnByMe;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up fields.
		wifiOnByMe = false;
		btOnByMe = false;

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

		if(btManager.isBTEnabled() && btOnByMe)
			btManager.disableBT();

		if(wifiManager.isWifiEnabled() && wifiOnByMe)
			setWifi(false);
	}

	@Override
	public void onDestroy(){
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
		if(request == REQUEST_ENABLE_BT && result == RESULT_OK){
			if(!wifiManager.isWifiEnabled())
				enableWifi();
		}else{
			Toast.makeText(this, R.string.bt_on_fail, Toast.LENGTH_LONG).show();
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
			intent.putExtra("address", ipAddress);
			startActivity(intent);
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

	private void enableWifi(){
		if(!wifiManager.isWifiEnabled()){
			DialogFragment wifiOn = new WifiOnDialog();
			((WifiOnDialog)wifiOn).setWifiManager(wifiManager);
			wifiOn.show(getFragmentManager(), "wifi_on");
		}
	}

	private void enableBT(){
		Logger.log_d(TAG, CLASS_NAME + ".enableBT() :: Enabling the Bluetooth radio.");
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		btOnByMe = true;
	}

	protected void showToast(int stringId, int length){
		Toast.makeText(this, stringId, length).show();
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
		protected Boolean doInBackground(Void... params){
			boolean result, done = false;
			byte[] buffer = (new String("Server is here")).getBytes();

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
					if(received.compareTo("NxtAR server here!") == 0)
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
			// If a server was found then start the next activity.
			if(packet != null)
				startCamActivity(result, packet.getAddress().getHostAddress());
			else
				startCamActivity(false, null);
		}
	}

	private class ConnectRobotTask extends AsyncTask<Void, Void, Boolean>{
		private final String CLASS_NAME = ConnectRobotTask.class.getSimpleName();
		private String macAddress;

		public ConnectRobotTask(String macAddress){
			this.macAddress = macAddress;
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
			if(result){
				Logger.log_d(TAG, CLASS_NAME + "doInBackground() :: Connection successful.");
				showToast(R.string.conn_established, Toast.LENGTH_SHORT);
			}else{
				Logger.log_d(TAG, CLASS_NAME + "doInBackground() :: Connection failed.");
				showToast(R.string.conn_failed, Toast.LENGTH_LONG);
				connectButton.setEnabled(true);
			}
		}
	}

	@Override
	public void onWifiOnDialogPositiveClick(DialogFragment dialog) {
		Toast.makeText(this, R.string.wifi_on_success, Toast.LENGTH_SHORT).show();
		wifiOnByMe = true;
	}

	@Override
	public void onWifiOnDialogNegativeClick(DialogFragment dialog) {
		Toast.makeText(this, R.string.wifi_on_fail, Toast.LENGTH_LONG).show();
		finish();
	};

	public void connectWithRobot(View view){
		if(btManager.isBTEnabled()){
			DialogFragment connectBot = new ConnectRobotDialog();
			connectBot.show(getFragmentManager(), "connect_bot");
		}
	}

	public void startConnections(View view){
		ServiceDiscoveryTask serviceDiscovery = new ServiceDiscoveryTask();
		serviceDiscovery.execute();
	}

	@Override
	public void onConnectRobotDialogItemClick(DialogFragment dialog, String item) {
		String macAddress = item.substring(item.indexOf('\n')+1);
		Logger.log_d(TAG, CLASS_NAME + ".onConnectRobotDialogItemClick() :: MAC address: " + macAddress);
		connectButton.setEnabled(false);
		ConnectRobotTask robotTask = new ConnectRobotTask(macAddress);
		robotTask.execute();
	}
}
