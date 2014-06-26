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
 * 
 * NOTE:
 * The getOptimalPreviewSize() function is copied and slightly modified from
 * the Android Open Source Project ApiDemos camera preview code available at
 * the following url http://goo.gl/thP1e4 from http://android.googlesource.com .
 * The ApiDemos code is Copyright (C) 2007 The Android Open Source Project also
 * avilable under the Apache License Version 2.0.
 */
package ve.ucv.ciens.ccg.nxtcam.camera;

import java.io.IOException;
import java.util.List;

import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private static final String TAG            = "SURFVIEW";
	private static final String CLASS_NAME     = CameraPreview.class.getSimpleName();
	private static final int    OPTIMAL_WIDTH  = 352;
	private static final int    OPTIMAL_HEIGHT = 288;

	private CameraImageMonitor imgMonitor;
	private Activity           parentActivity;
	private SurfaceHolder      holder;
	private Camera             camera;
	private int                previewWidth;
	private int                previewHeight;

	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera){
		super(context);
		parentActivity = (Activity)context;

		holder = getHolder();
		holder.addCallback(this);

		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB)
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * <p>Sets the camera for this surface view.</p>
	 * 
	 * @param camera The camera to set.
	 */
	public void setCamera(Camera camera){
		this.camera = camera;
		if(this.camera != null){
			Logger.log_d(TAG, CLASS_NAME + ".setCamera(): Setting camera.");
			imgMonitor = CameraImageMonitor.getInstance();
			requestLayout();
			Logger.log_d(TAG, CLASS_NAME + ".setCamera(): Camera set.");
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder){
		// The Surface has been created, now tell the camera where to draw the preview.
		Logger.log_d(TAG, CLASS_NAME + ".surfaceCreated(): Creating surface view.");
		try {
			if(camera != null)
				camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			Logger.log_e(TAG, CLASS_NAME + ".surfaceCreated(): Error creating camera: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder){
		if(camera != null)
			camera.stopPreview();
	}

	@Override
	public void surfaceChanged(SurfaceHolder tmpHolder, int format, int w, int h){
		int        result;
		int        rotation;
		int        degrees = 0;
		Size       optimal;
		CameraInfo info;
		List<Size> sizes;
		Parameters camParams;

		Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Method started.");
		if(this.holder.getSurface() == null || camera == null){
			Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Holder and/or camera are null.");
			return;
		}

		try{ camera.stopPreview(); }catch (Exception e){ }

		Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Request layout.");
		requestLayout();

		Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Get parameters.");
		camParams = camera.getParameters();
		sizes = camParams.getSupportedPreviewSizes();

		for(Size size: sizes){
			Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Supported preview size (" + size.width + ", " + size.height + ")");
		}

		optimal = getOptimalPreviewSize(sizes, camera.new Size(OPTIMAL_WIDTH, OPTIMAL_HEIGHT));
		Logger.log_d(TAG, CLASS_NAME + ".surfaceChanged(): Preview size set at (" + optimal.width + ", " + optimal.height + ")");
		camParams.setPreviewSize(optimal.width, optimal.height);
		camera.setParameters(camParams);
		previewWidth = optimal.width;
		previewHeight = optimal.height;

		info = new CameraInfo();
		Camera.getCameraInfo(0, info);
		rotation = parentActivity.getWindowManager().getDefaultDisplay().getRotation();

		switch (rotation) {
		default:
		case Surface.ROTATION_0:
			degrees = 0;
			break;

		case Surface.ROTATION_90:
			degrees = 90;
			break;

		case Surface.ROTATION_180:
			degrees = 180;
			break;

		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
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
			Logger.log_e(TAG, CLASS_NAME + ".surfaceChanged(): Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera){
		if(imgMonitor.hasChanged())
			imgMonitor.setImageParameters(previewWidth, previewHeight);
		imgMonitor.setImageData(data);
	}

	/**
	 * <p>If camera is not null then disable the camera preview.</p>
	 */
	public void removePreviewCallback(){
		if(camera != null)
			camera.setPreviewCallback(null);
	}

	/**
	 * <p>Tries to obtain the preview size that better matches the target size pixel dimensions.</p>
	 * 
	 * @param sizes An initialized list of supported preview sizes.
	 * @param target The ideal preview size.
	 * @return The size from the list of sizes that better matches the target. Null if sizes is null or empty.
	 */
	private Size getOptimalPreviewSize(List<Size> sizes, final Size target) {
		final int    TARGET_HEIGHT    = target.height;
		Size         optimalSize      = null;
		double       minDiff          = Double.MAX_VALUE;

		if (sizes == null || sizes.size() == 0) return null;

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			Logger.log_d(TAG, CLASS_NAME + ".getOptimalPreviewSize(): Ignoring aspect ratio.");
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - TARGET_HEIGHT) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - TARGET_HEIGHT);
					Logger.log_d(TAG, CLASS_NAME + ".getOptimalPreviewSize(): Size = (" + Integer.toString(size.width) + ", " + Integer.toString(size.width) + ")");
					Logger.log_d(TAG, CLASS_NAME + ".getOptimalPreviewSize(): minDiff = " + Double.toString(minDiff));
				}
			}
		}

		Logger.log_d(TAG, CLASS_NAME + ".getOptimalPreviewSize(): Optimal size found is (" + Integer.toString(optimalSize.width) + ", " + Integer.toString(optimalSize.height) + ")");

		return optimalSize;
	}
}
