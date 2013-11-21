package ve.ucv.ciens.ccg.nxtcam.utils;

import android.util.Log;

public abstract class Logger {
	public static void log_d(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.d(tag, msg);
	}

	public static void log_i(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.i(tag, msg);
	}

	public static void log_v(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.v(tag, msg);
	}

	public static void log_w(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.w(tag, msg);
	}

	public static void log_e(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.e(tag, msg);
	}

	public static void log_wtf(String tag, String msg){
		if(ProjectConstants.DEBUG) Log.wtf(tag, msg);
	}
}
