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
package ve.ucv.ciens.ccg.nxtcam.camera;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.graphics.Rect;

public class CameraImageMonitor{
	private final String TAG = "CAM_MONITOR";
	private final String CLASS_NAME = CameraImageMonitor.class.getSimpleName();

	private Object imageMonitor;
	private byte[] image;
	private boolean imageProduced;
	private boolean imageConsumed;
	private Rect imageSize;

	private CameraImageMonitor(){
		imageProduced = false;
		imageConsumed = true;
		imageMonitor = new Object();
		imageSize = null;
	}

	private static class SingletonHolder{
		public static final CameraImageMonitor INSTANCE = new CameraImageMonitor();
	}

	public static CameraImageMonitor getInstance(){
		return SingletonHolder.INSTANCE;
	}

	public void setImageParameters(int width, int height){
		imageSize = new Rect(0, 0, width, height);
	}
	
	public Rect getImageParameters(){
		return imageSize;
	}
	
	public void setImageData(byte[] image){
		if(imageConsumed){
			Logger.log_d(TAG, CLASS_NAME + ".setImageData() :: Copying new image.");
			synchronized(this.imageMonitor){
				this.image = image;
				imageProduced = true;
				imageConsumed = false;
				this.imageMonitor.notifyAll();
			}
			Logger.log_d(TAG, CLASS_NAME + ".setImageData() :: Data copy finished.");
		}else{
			Logger.log_d(TAG, CLASS_NAME + ".setImageData() :: Old image still valid, ignoring new image.");
		}
	}

	public byte[] getImageData(){
		byte[] returnImg;
		Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: Entry point.");
		synchronized(imageMonitor){
			while(!imageProduced){
				Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: Waiting for new image.");
				try{ imageMonitor.wait(); }catch(InterruptedException ie){ }
			}
			Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: Retrieving new image.");
			returnImg = image;
			imageProduced = false;
			imageConsumed = true;
		}
		Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: New image retrieved.");
		return returnImg;
	}

	public synchronized boolean hasChanged(){
		return imageConsumed;
	}
}
