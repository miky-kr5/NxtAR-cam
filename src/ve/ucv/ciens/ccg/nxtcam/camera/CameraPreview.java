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

	private Size previewSize;
	private List<Size> supportedPreviewSizes;
	private CameraImageMonitor imgMonitor;
	private Activity parentActivity;
	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	private Camera camera;

	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera){
		super(context);
		parentActivity = (Activity)context;

		surfaceView = new SurfaceView(context);
		holder = surfaceView.getHolder();
		holder.addCallback(this);

		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB)
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera){
		this.camera = camera;
		if(this.camera != null){
			imgMonitor = CameraImageMonitor.getInstance();
			supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
			requestLayout();
		}
	}

	public void surfaceCreated(SurfaceHolder holder){
		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			if(camera != null)
				camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			Logger.log_e(TAG, CLASS_NAME + ".surfaceCreated() :: Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder){
		if(camera != null)
			camera.stopPreview();
	}

	public void surfaceChanged(SurfaceHolder tmpHolder, int format, int w, int h){
		int result;
		int rotation;
		int degrees = 0;
		Camera.Parameters camParams;

		Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged() :: Method started.");
		if(this.holder.getSurface() == null || camera == null){
			Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged() :: Holder and/or camera are null.");
			return;
		}

		try{ camera.stopPreview(); }catch (Exception e){ }

		requestLayout();

		camParams = camera.getParameters();
		/*Size optimal = getOptimalPreviewSize(camParams.getSupportedPreviewSizes(), w, h);
		if(ProjectConstants.DEBUG)
			Log.d(TAG, CLASS_NAME + ".surfaceChanged() :: Preview size set at (" + optimal.width + ", " + optimal.height + ")");*/
		camParams.setPreviewSize(previewSize.width, previewSize.height);
		camera.setParameters(camParams);

		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(0, info);
		rotation = parentActivity.getWindowManager().getDefaultDisplay().getRotation();

		switch (rotation) {
		case Surface.ROTATION_0: degrees = 0; break;
		case Surface.ROTATION_90: degrees = 90; break;
		case Surface.ROTATION_180: degrees = 180; break;
		case Surface.ROTATION_270: degrees = 270; break;
		}

		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
		camera.setPreviewCallback(this);

		try {
			camera.setPreviewDisplay(this.holder);
			camera.startPreview();
		}catch (Exception e){
			Logger.log_e(TAG, CLASS_NAME + ".surfaceChanged() :: Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera){
		Logger.log_d(TAG, CLASS_NAME + ".onPreviewFrame() :: Preview received");
		Logger.log_d(TAG, CLASS_NAME + ".onPreviewFrame() :: Frame has" + (imgMonitor.hasChanged() ? "" : " not") + " changed.");
		if(!imgMonitor.hasChanged())
			imgMonitor.setImageData(data);
	}

	public void removePreviewCallback(){
		if(camera != null)
			camera.setPreviewCallback(null);
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
			if (previewSize != null) {
				previewWidth = previewSize.width;
				previewHeight = previewSize.height;
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

		if (supportedPreviewSizes != null) {
			previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
		}
	}
}
