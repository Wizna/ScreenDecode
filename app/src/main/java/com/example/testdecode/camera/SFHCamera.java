package com.example.testdecode.camera;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

import com.example.testdecode.activity.DecodeActivity;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
public class SFHCamera implements SurfaceHolder.Callback {
    public DecodeActivity parentActivity;
    public Camera mCamera;
    public Size mPreviewSize = null;
    private SurfaceHolder holder = null;
    private int width, height;
    private Camera.PreviewCallback previewCallback;
    private Camera.ErrorCallback errorCallback;
    private boolean flag_StartPre = false;
    private Camera.AutoFocusCallback mAutoFocusCallBack = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mCamera != null) {
                AutoFocusAndPreviewCallback();
            }
        }
    };

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
            mCamera.setDisplayOrientation(90);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.set("rotation", "90");
            mCamera.setErrorCallback(this.errorCallback);
            mCamera.setPreviewDisplay(holder);
            Log.i("Camera", "surfaceCreated");

            List<Size> mSupportedPreviewSizes;
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, height, width);
            Log.i("SFHCamera", width + " " + height + " w: " + mPreviewSize.width + " h: " + mPreviewSize.height);

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
            } catch (Exception ex) {
                Log.i("autofocus faild cause:", ex.getMessage());
            }
        }
    }

    public boolean checkCameraIsOK() {
        return mCamera != null;
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

    public void startGetImgFrame() {
        try {
            mCamera.setOneShotPreviewCallback(previewCallback);
        } catch (Exception e) {
            Log.e("camera-1", e.getMessage());
        }
    }


    ///////////////////////////////////////////////////
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            Log.i("SFHCamera", "Sizes: " + size.width + " " + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

}
