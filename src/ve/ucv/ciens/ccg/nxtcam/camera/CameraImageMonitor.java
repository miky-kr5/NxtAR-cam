package ve.ucv.ciens.ccg.nxtcam.camera;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;

public class CameraImageMonitor{
	private final String TAG = "CAM_MONITOR";
	private final String CLASS_NAME = CameraImageMonitor.class.getSimpleName();

	private Object imageMonitor;
	private byte[] image;
	private boolean imgProduced;
	private boolean imgConsumed;

	private CameraImageMonitor(){
		imgProduced = false;
		imgConsumed = true;
		imageMonitor = new Object();
	}

	private static class SingletonHolder{
		public static final CameraImageMonitor INSTANCE = new CameraImageMonitor();
	}

	public static CameraImageMonitor getInstance(){
		return SingletonHolder.INSTANCE;
	}

	public void setImageData(byte[] image){
		if(imgConsumed){
			Logger.log_d(TAG, CLASS_NAME + ".setImageData() :: Copying new image.");
			synchronized(this.imageMonitor){
				this.image = image;
				imgProduced = true;
				imgConsumed = false;
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
			while(!imgProduced){
				Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: Waiting for new image.");
				try{ imageMonitor.wait(); }catch(InterruptedException ie){ }
			}
			Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: Retrieving new image.");
			returnImg = image;
			imgProduced = false;
			imgConsumed = true;
		}
		Logger.log_d(TAG, CLASS_NAME + ".getImageData() :: New image retrieved.");
		return returnImg;
	}

	public synchronized boolean hasChanged(){
		return imgConsumed;
	}
}
