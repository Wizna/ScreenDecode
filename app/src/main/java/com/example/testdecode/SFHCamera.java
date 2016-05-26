package com.example.testdecode;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class SFHCamera implements SurfaceHolder.Callback {
    public testdecode parentActivity;
    private SurfaceHolder holder = null;
    public Camera mCamera;
    private int width, height;
    private Camera.PreviewCallback previewCallback;
    private Camera.ErrorCallback errorCallback;

    private boolean flag_StartPre = false;
    public Size mPreviewSize = null;

    public SFHCamera(SurfaceHolder holder, int w, int h, Camera.PreviewCallback previewCallback, Camera.ErrorCallback errorCallback) {
        this.holder = holder;
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        width = w;
        height = h;
        this.previewCallback = previewCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        if (android.os.Build.MODEL.equals("HUAWEI T8300")) {
            createCamera(PixelFormat.YCbCr_422_SP);
        } else {
            createCamera(ImageFormat.NV21);
        }

    }

    public void createCamera(int pixelFormat) {
        try {
            if (mCamera != null) {
                this.releaseCamera();
            }
            mCamera = Camera.open();
            Camera.Parameters parameters = mCamera.getParameters();
            mCamera.setErrorCallback(this.errorCallback);
            mCamera.setPreviewDisplay(holder);
            Log.i("Camera", "surfaceCreated");

            List<Size> mSupportedPreviewSizes;
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            /*

		    List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
		    for(int i=0;i<previewFormats.size();i++)
		    {
		    	if(previewFormats.get(i)==PixelFormat.YCbCr_420_SP||previewFormats.get(i)==PixelFormat.YCbCr_422_SP||previewFormats.get(i)==PixelFormat.YCbCr_422_I)
		    	{
		    		pixelFormat=previewFormats.get(i);
		    		Log.i("get the type of data:",String.valueOf(previewFormats.get(i)));
		    		break;
		    	}
		    }
		    */

            parameters.setPreviewSize((mPreviewSize.width == 0 ? 480 : mPreviewSize.width), (mPreviewSize.height == 0 ? 320 : mPreviewSize.height));
            parameters.setPreviewFormat(pixelFormat);
            parameters.setFlashMode(!this.parentActivity.flag_light ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            mCamera.startPreview();

            flag_StartPre = true;
            Log.i("Camera", "surfaceChanged");
            this.parentActivity.Start();
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            Log.e("camera-1", e.getMessage());
        }
    }


    public void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.setErrorCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception ex) {
            Log.e("camera-1", ex.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        if (mCamera != null) {
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera = null;
            Log.i("Camera", "surfaceDestroyed");
        }
    }


    public void AutoFocusAndPreviewCallback() {
        if (mCamera != null) {
            try {

                if (!flag_StartPre) {
                    mCamera.startPreview();
                }

                mCamera.autoFocus(mAutoFocusCallBack);
//				Log.i("Camera","autofocus");
            } catch (Exception ex) {
                Log.i("autofocus faild cause:", ex.getMessage().toString());
            }
        }
    }

    public boolean checkCameraIsOK() {
        if (mCamera == null) {
            return false;
        } else {
            return true;
        }
    }

    public void cancelFocus() {
        try {
            if (mCamera != null) {
                mCamera.cancelAutoFocus();
            }
        } catch (Exception ex) {
            Log.e("camera-1", ex.getMessage());
        }

    }


    private Camera.AutoFocusCallback mAutoFocusCallBack = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            //if (success)
            //{ 
//            	 Log.i("Camera","autofocus_success");
            if (mCamera != null) {
                //mCamera.setOneShotPreviewCallback(previewCallback);
                AutoFocusAndPreviewCallback();
            }
            //}
        }
    };

    public void startGetImgFrame() {
        try {
            mCamera.setOneShotPreviewCallback(previewCallback);
        } catch (Exception e) {
            Log.e("camera-1", e.getMessage());
        }
    }


    public void takePicture() {
        mCamera.takePicture(null, null, null);
    }


    ///////////////////////////////////////////////////
    private Size getOptimalPreviewSize(List<Size> sizes, int h, int w) {
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

}
