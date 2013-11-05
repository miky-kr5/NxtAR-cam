package ve.ucv.ciens.ccg.nxtcam;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final String TAG = "NXTCAM_MAIN";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();

	private Button startButton;
	private TextView ipField;

	private WifiManager wifiManager;
	private boolean wifiOnByMe;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(startClickListener);

		ipField = (TextView)findViewById(R.id.ipAddressField);

		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

		if(!wifiManager.isWifiEnabled()) setWifi(true);
	}

	@Override
	public void onResume(){
		super.onResume();

		if(!wifiManager.isWifiEnabled()) setWifi(true);
	}

	@Override
	public void onPause(){
		super.onPause();

		if(wifiManager.isWifiEnabled() && wifiOnByMe) setWifi(false);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();

		if(wifiManager.isWifiEnabled() && wifiOnByMe) wifiManager.setWifiEnabled(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void startCamActivity(boolean canStart){
		if(canStart){
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".startCamActivity() :: Launching camera activity.");
			Intent intent = new Intent(this, CamActivity.class);
			startActivity(intent);
		}else{
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".startCamActivity() :: Cannot launch camera activity.");
			Toast.makeText(this, R.string.badIpToast, Toast.LENGTH_SHORT).show();
		}
	}

	private void setWifi(boolean on){
		wifiManager.setWifiEnabled(on);
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".ImageTransferThread() :: setting wifi to " + (on ? "on" : "off"));
		if(on)
			wifiOnByMe = true;
		else
			wifiOnByMe = false;
	}

	private void validateIpAddress(){
		if(ipField.getText().toString().compareTo("") != 0){
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "validateIpAddress() :: Launching verification task.");
			VerifyIpAddressTask verifyIp = new VerifyIpAddressTask();
			verifyIp.execute(ipField.getText().toString());
		}else{
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + "validateIpAddress() :: Ip address field is empty.");
			Toast.makeText(this, R.string.emptyIpToast, Toast.LENGTH_SHORT).show();
		}
	}

	private final View.OnClickListener startClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			validateIpAddress();
		}
	};

	private class VerifyIpAddressTask extends AsyncTask<String, Void, Boolean>{
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
	};
}
