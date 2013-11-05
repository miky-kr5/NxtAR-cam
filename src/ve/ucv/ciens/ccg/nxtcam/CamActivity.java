package ve.ucv.ciens.ccg.nxtcam;

import ve.ucv.ciens.ccg.nxtcam.camera.CameraPreview;
import ve.ucv.ciens.ccg.nxtcam.network.ImageTransferThread;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class CamActivity extends Activity{
	private final String TAG = "NXTCAM_CAM";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();
	
	private Camera hwCamera;
	private CameraPreview cPreview;
	private CameraSetupTask camSetupTask;
	private ImageTransferThread imThread;
	private String serverIp;

	/*******************
	 * Android methods *
	 *******************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		cPreview = new CameraPreview(this, hwCamera);
		setContentView(cPreview);
		
		Intent intent = getIntent();
		serverIp = intent.getStringExtra("address");
		
		imThread = new ImageTransferThread();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.cam, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume(){
		super.onResume();

		camSetupTask = new CameraSetupTask();
		camSetupTask.execute();
	}

	@Override
	public void onPause(){
		super.onPause();

		cPreview.removePreviewCallback();
		cPreview.setCamera(null);
		releaseCamera();
	}

	/******************
	 * My own methods *
	 ******************/	
	public void startCameraPreview(){
		if(hwCamera != null){
			cPreview.setCamera(hwCamera);
		}else{
			Log.wtf(TAG, CLASS_NAME + ".startCameraPreview() :: CAMERA IS NULL!");
			System.exit(1);
		}
	}

	private void releaseCamera(){
		if(hwCamera != null){
			hwCamera.release();
			hwCamera = null;
		}
	}

	private class CameraSetupTask extends AsyncTask<Void, Void, Camera>{
		private final String CLASS_NAME = CameraSetupTask.class.getSimpleName();

		@Override
		protected Camera doInBackground(Void... params) {
			Camera cam = null;
			try{
				cam = Camera.open(0);
			}catch(Exception e){
				if(ProjectConstants.DEBUG) Log.e(TAG, CLASS_NAME + ".doInBackground() :: Failed to open the camera.");
			}
			return cam;
		}

		@Override
		protected void onPostExecute(Camera result) {
			hwCamera = result;
			if(result != null){
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".onPostExecute() :: Camera successfully opened");
			}else{
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".onPostExecute() :: Camera open failed on background task.");
				Toast.makeText(getApplicationContext(), R.string.camera_failure, Toast.LENGTH_LONG).show();
			}
			startCameraPreview();

			super.onPostExecute(result);
		}
	};
}
