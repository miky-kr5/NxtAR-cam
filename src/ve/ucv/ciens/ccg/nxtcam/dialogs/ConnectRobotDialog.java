package ve.ucv.ciens.ccg.nxtcam.dialogs;

import ve.ucv.ciens.ccg.nxtcam.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ConnectRobotDialog extends DialogFragment {
	private final String TAG = "NXTCAM_ROBOT_DIALOG";
	private final String CLASS_NAME = ConnectRobotDialog.class.getSimpleName();

	private ConnectRobotDialogListener listener;

	public interface ConnectRobotDialogListener{
		public void onConnectRobotDialogPositiveClick(DialogFragment dialog);
		public void onConnectRobotDialogNegativeClick(DialogFragment dialog);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setMessage(R.string.wifi_on_msg).setPositiveButton(R.string.wifi_on_button, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id){

			}

		}).setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id){

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
