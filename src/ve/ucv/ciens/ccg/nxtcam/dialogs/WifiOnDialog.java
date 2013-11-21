package ve.ucv.ciens.ccg.nxtcam.dialogs;

import ve.ucv.ciens.ccg.nxtcam.R;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class WifiOnDialog extends DialogFragment {
	private final String TAG = "NXTCAM_MAIN";
	private final String CLASS_NAME = WifiOnDialog.class.getSimpleName();

	private WifiManager wifiManager;

	public interface WifiOnDialogListener{
		public void onWifiOnDialogPositiveClick(DialogFragment dialog);
		public void onWifiOnDialogNegativeClick(DialogFragment dialog);
	}

	public void setWifiManager(WifiManager wifiManager){
		this.wifiManager = wifiManager;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setMessage(R.string.wifi_on_msg).setPositiveButton(R.string.wifi_on_button, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				if(wifiManager != null){
					wifiManager.setWifiEnabled(true);
					Logger.log_d(TAG, CLASS_NAME + " :: setting wifi to on.");
				}else
					Logger.log_wtf(TAG, CLASS_NAME + " :: wifiManager is null! Doing nothing.");
			}

		}).setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) { }

		});

		// Create the AlertDialog object and return it
		return builder.create();
	}

	@Override
	public void onAttach(Activity activity){
		try{
			@SuppressWarnings("unused")
			WifiOnDialogListener listener = (WifiOnDialogListener)activity;
		}catch(ClassCastException cce){
			throw new ClassCastException(activity.toString() + "Must implement WifiOnDialogListener.");
		}
	}
}
