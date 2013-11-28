package ve.ucv.ciens.ccg.nxtcam.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ve.ucv.ciens.ccg.nxtcam.R;
import ve.ucv.ciens.ccg.nxtcam.network.BTCommunicator;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import ve.ucv.ciens.ccg.nxtcam.utils.ProjectConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;

public class ConnectRobotDialog extends DialogFragment {
	private final String TAG = "NXTCAM_ROBOT_DIALOG";
	private final String CLASS_NAME = ConnectRobotDialog.class.getSimpleName();

	private ConnectRobotDialogListener listener;
	private BTCommunicator btManager;
	private List<String> devices;
	private String[] devicesArray;

	public interface ConnectRobotDialogListener{
		public void onConnectRobotDialogItemClick(DialogFragment dialog, String item);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Fill a list with all the paired LEGO robots.
		btManager = BTCommunicator.getInstance();
		devices = new ArrayList<String>();
		Set<BluetoothDevice> pairedDevices = btManager.getPairedDevices();
		for (BluetoothDevice device : pairedDevices) {
			// Put the device in the list only if it's MAC address belongs to LEGO.
			if(device.getAddress().substring(0, 8).compareTo(ProjectConstants.OUI_LEGO) == 0)
				devices.add(device.getName() + "\n" + device.getAddress());
		}
		devicesArray = devices.toArray(new String[devices.size()]);

		builder.setTitle(R.string.robot_choice).setItems(devicesArray, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which){
				Logger.log_d(TAG, CLASS_NAME + ".setItems().onClick( :: User chose: " + devices.get(which));
				listener.onConnectRobotDialogItemClick(ConnectRobotDialog.this, devices.get(which));
			}

		}).setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id){
				Logger.log_d(TAG, CLASS_NAME + ".setNegativeButton().onClick( :: User canceled.");
			}

		});

		// Create the AlertDialog object and return it
		return builder.create();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);

		try{
			listener = (ConnectRobotDialogListener)activity;
		}catch(ClassCastException cce){
			listener = null;
			throw new ClassCastException(CLASS_NAME + ".onAttach() :: " + activity.toString() + "Must implement WifiOnDialogListener.");
		}
	}
}
