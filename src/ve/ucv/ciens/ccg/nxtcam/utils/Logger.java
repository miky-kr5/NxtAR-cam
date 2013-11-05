package ve.ucv.ciens.ccg.nxtcam.utils;

import android.util.Log;

public abstract class Logger {
	public static enum LOG_TYPES{ DEBUG, INFO, WARN, ERROR, VERBOSE, WTF }
	
	public static void log(LOG_TYPES log_type, String tag, String msg){
		if(ProjectConstants.DEBUG){
			switch(log_type){
			case DEBUG:
				Log.d(tag, msg);
				break;
			case INFO:
				Log.i(tag, msg);
				break;
			case WARN:
				Log.w(tag, msg);
				break;
			case ERROR:
				Log.e(tag, msg);
				break;
			case VERBOSE:
				Log.v(tag, msg);
				break;
			case WTF:
				Log.wtf(tag, msg);
				break;
			}
		}
	}
}
