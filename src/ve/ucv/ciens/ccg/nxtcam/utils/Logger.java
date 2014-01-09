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
