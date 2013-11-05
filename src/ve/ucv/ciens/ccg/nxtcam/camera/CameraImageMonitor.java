package ve.ucv.ciens.ccg.nxtcam.camera;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;

public class CameraImageMonitor{
	private final String TAG = "CAM_MONITOR";
	private final String CLASS_NAME = CameraImageMonitor.class.getSimpleName();

	private byte[] image;
	private boolean imgChanged;

	private CameraImageMonitor(){
		imgChanged = false;
	}

	private static class SingletonHolder{
		public static final CameraImageMonitor INSTANCE = new CameraImageMonitor();
	}

	public static CameraImageMonitor getInstance(){
		return SingletonHolder.INSTANCE;
	}

	public void setImageData(byte[] image){
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".setImageData() :: Copying new image.");
		synchronized(image){
			this.image = new byte[image.length];
			System.arraycopy(image, 0, this.image, 0, image.length);
			imgChanged = true;
			image.notifyAll();
		}
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".setImageData() :: Data copy finished.");
	}

	public byte[] getImageData(){
		byte[] returnImg;
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".getImageData() :: Entry point.");
		synchronized(image){
			while(!imgChanged){
				Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".getImageData() :: Waiting for new data.");
				try{ image.wait(); }catch(InterruptedException ie){}
			}
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".getImageData() :: Retrieving new data.");
			returnImg = image.clone();
			imgChanged = false;
		}
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".getImageData() :: New data retreived.");
		return returnImg;
	}

	public synchronized boolean hasChanged(){
		return imgChanged;
	}
}
