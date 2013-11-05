package ve.ucv.ciens.ccg.nxtcam.camera;

import java.io.IOException;
import java.util.List;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/** A basic Camera preview class */
@SuppressLint("ViewConstructor")
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private final String TAG = "SURFVIEW";
	private final String CLASS_NAME = CameraPreview.class.getSimpleName();

	private Size mPreviewSize;
    private List<Size> mSupportedPreviewSizes;
	private CameraImageMonitor camMonitor;
	private Activity parentActivity;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private Camera mCamera;

	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera){
		super(context);
		parentActivity = (Activity)context;

		mSurfaceView = new SurfaceView(context);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);

		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB)
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	public void setCamera(Camera camera){
		mCamera = camera;
		if(mCamera != null){
			camMonitor = CameraImageMonitor.getInstance();
			mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
			requestLayout();
		}
	}

	public void surfaceCreated(SurfaceHolder holder){
		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			if(mCamera != null){
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException e) {
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder){
		if(mCamera != null){
			mCamera.stopPreview();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
		if(mHolder.getSurface() == null){
			return;
		}

		try{
			mCamera.stopPreview();
		}catch (Exception e){ }

		requestLayout();
		
		Camera.Parameters camParams = mCamera.getParameters();
		/*Size optimal = getOptimalPreviewSize(camParams.getSupportedPreviewSizes(), w, h);
		if(ProjectConstants.DEBUG)
			Log.d(TAG, CLASS_NAME + ".surfaceChanged() :: Preview size set at (" + optimal.width + ", " + optimal.height + ")");*/
		camParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		mCamera.setParameters(camParams);

		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(0, info);
		int rotation = parentActivity.getWindowManager().getDefaultDisplay().getRotation();

		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0: degrees = 0; break;
		case Surface.ROTATION_90: degrees = 90; break;
		case Surface.ROTATION_180: degrees = 180; break;
		case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		mCamera.setDisplayOrientation(result);

		mCamera.setPreviewCallback(this);
		
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		}catch (Exception e){
			Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".surfaceChanged() :: Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera){
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".onPreviewFrame() :: Preview received");
		Logger.log(Logger.LOG_TYPES.DEBUG, TAG, CLASS_NAME + ".onPreviewFrame() :: Frame has" + (camMonitor.hasChanged() ? "" : " not") + " changed.");
		if(!camMonitor.hasChanged())
			camMonitor.setImageData(data);
	}

	public void removePreviewCallback(){
		mCamera.setPreviewCallback(null);
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }
}
