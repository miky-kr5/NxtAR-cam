package ve.ucv.ciens.ccg.nxtcam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
public class MainActivity extends Activity {
	// Cosntant fields.
	private final String TAG = "NXTCAM_MAIN";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();

	// Gui components.
	private Button startButton;
	//private TextView ipField;

	// Resources.
	private WifiManager wifiManager;

	// Variables.
	private boolean wifiOnByMe;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up fields.
		wifiOnByMe = false;

		// Set up gui components.
		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(startClickListener);
		// ipField = (TextView)findViewById(R.id.ipAddressField);

		// Set up services.
		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if(!wifiManager.isWifiEnabled())
			setWifi(true);
	}

	@Override
	public void onResume(){
		super.onResume();

		if(!wifiManager.isWifiEnabled())
			setWifi(true);
	}

	@Override
	public void onPause(){
		super.onPause();

		if(wifiManager.isWifiEnabled() && wifiOnByMe)
			setWifi(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Start the camera capture activity if a server was found through service discovery.
	 * 
	 * @param serverFound Indicates if a server was found, doh!
	 * @param ipAddress The ip address of the server.
	 */
	private void startCamActivity(boolean serverFound, String ipAddress){
		if(serverFound){
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".startCamActivity() :: Launching camera activity.");
			Intent intent = new Intent(this, CamActivity.class);
			intent.putExtra("address", ipAddress);
			startActivity(intent);
		}else{
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".startCamActivity() :: Cannot launch camera activity.");
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
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".setWifi() :: setting wifi to " + (radioState ? "on" : "off"));
		if(radioState)
			wifiOnByMe = true;
		else
			wifiOnByMe = false;
	}

	/*private void validateIpAddress(){
		if(ipField.getText().toString().compareTo("") != 0){
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "validateIpAddress() :: Launching verification task.");
			VerifyIpAddressTask verifyIp = new VerifyIpAddressTask();
			verifyIp.execute(ipField.getText().toString());
		}else{
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "validateIpAddress() :: Ip address field is empty.");
			Toast.makeText(this, R.string.emptyIpToast, Toast.LENGTH_SHORT).show();
		}
	}*/

	/**
	 * Event listener for the connection button.
	 */
	private final View.OnClickListener startClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//validateIpAddress();
			ServiceDiscoveryTask serviceDiscovery = new ServiceDiscoveryTask();
			serviceDiscovery.execute();
		}
	};

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
				Logger.log(Logger.LOG_TYPES.ERROR, TAG ,CLASS_NAME + ".ServiceDiscoveryTask() :: " + io.getMessage());
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
					Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".run() :: Found a server at " + packet.getAddress().getHostAddress());
					String received = new String(packet.getData());
					if(received.compareTo("NxtAR server here!") == 0)
						done = true;
				}
				result = true;
			}catch(IOException io){
				Logger.log(Logger.LOG_TYPES.ERROR, TAG, CLASS_NAME + ".doInBackground() :: " + io.getMessage());
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
	};

	/*	private class VerifyIpAddressTask extends AsyncTask<String, Void, Boolean>{
		private final String CLASS_NAME = VerifyIpAddressTask.class.getSimpleName();

		@Override
		protected Boolean doInBackground(String... params) {
			try{
				InetAddress.getByName(params[0]);
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "doInBackground() :: IP address is valid.");
				return true;
			}catch(UnknownHostException uh){
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "doInBackground() :: IP address is not valid.");
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			startCamActivity(result);
		}
	};*/
}
