package com.example.testdecode.activity;

import android.app.Activity;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.testdecode.MyApplication;
import com.example.testdecode.R;
import com.example.testdecode.camera.SFHCamera;
import com.example.testdecode.util.DecodeUtils;
import com.google.zxing.Android.PlanarYUVLuminanceSource;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

import java.io.ByteArrayOutputStream;


@SuppressWarnings("deprecation")
public class DecodeActivity extends Activity implements Runnable {

    private final String PREFERENCES_NAME = "decodesetting";
    private final String TAG = "DecodeActivity";
    public boolean flag_light = false;
    int width;
    int height;
    int dstLeft = 0;
    int dstTop = 0;
    int dstWidth = 0;
    int dstHeight = 0;
    int screenWidth;
    int screenHeight;
    AudioManager audioManager;
    Bitmap mBitmap;
    PlanarYUVLuminanceSource source;
    byte[] tempData;
    Thread t1;
    private SurfaceView sfvCamera;
    private SFHCamera sfhCamera;
    private ImageView imgView;
    private int m_nImgProcessTh;
    private int m_nPixDownDosTh;
    private boolean m_bCMYKMode;
    private boolean m_bInvertColorMode;
    private LinearLayout literCenter;
    private LinearLayout literImg;
    private int flag_DecodeMode = 0;
    private boolean flag_IsGetImg = true;
    private DecodeThread myDecodeThread;
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera arg1) {
            Log.i(TAG, "PreviewCallback");
            try {
                if (data != null) {
                    try {
                        if (myDecodeThread.getState().equals(Thread.State.NEW)) {
                            tempData = data;
                            myDecodeThread.start();
                        } else if (myDecodeThread.getState().equals(Thread.State.TERMINATED)) {
                            tempData = data;
                            myDecodeThread.run();
                        }

                        if (flag_IsGetImg) {
                            startGetImgFrame();
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "decode " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "camera-13 " + e.getMessage());
            }
        }
    };
    private Camera.ErrorCallback errorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            try {
                sfhCamera = null;
                sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight, previewCallback, errorCallback);
                sfhCamera.createCamera(ImageFormat.NV21);
                Log.i(TAG, "Here is the error call back");
            } catch (Exception ex) {
                Log.i(TAG, ex.getMessage());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.testdecode);

        audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        Log.i(TAG, "screen width" + String.valueOf(screenWidth));
        Log.i(TAG, "screen height" + String.valueOf(screenHeight));

        literCenter = (LinearLayout) findViewById(R.id.literCenter);
        literImg = (LinearLayout) findViewById(R.id.literImg);

        imgView = (ImageView) this.findViewById(R.id.ImageView01);
        sfvCamera = (SurfaceView) this.findViewById(R.id.sfvCamera);

        sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight, previewCallback, errorCallback);
        sfhCamera.parentActivity = this;

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        m_nImgProcessTh = preferences.getInt("ImgProcessTh", 6);
        m_nPixDownDosTh = preferences.getInt("DotsDownTh", 4);
        m_bCMYKMode = preferences.getBoolean("BCMYKMode", false);
        m_bInvertColorMode = preferences.getBoolean("BInvertColor", false);
        flag_DecodeMode = preferences.getInt("CodeType", 0);

        t1 = new Thread(this);
        myDecodeThread = new DecodeThread();
    }

    public void Start() {
        Log.i(TAG, "start()");
        if (t1.getState().equals(Thread.State.NEW)) {
            t1.start();
        } else if (t1.getState().equals(Thread.State.TERMINATED)) {
            t1.run();
        }
    }

    public void startGetImgFrame() {
        Log.i(TAG, "startGetImgFrame: " + String.valueOf(System.currentTimeMillis()));
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dstLeft = (literImg.getTop() + imgView.getTop()) * width / metrics.heightPixels;
        dstTop = (literCenter.getLeft() + imgView.getLeft()) * height / metrics.widthPixels;
        dstWidth = (imgView.getBottom() - imgView.getTop()) * width / metrics.heightPixels;
        dstHeight = (imgView.getRight() - imgView.getLeft()) * height / metrics.widthPixels;
        while ((dstLeft & 0x03) > 0) {
            dstLeft++;
        }
        if ((dstTop & 0x01) > 0) {
            dstTop++;
        }
        while ((dstWidth & 0x03) > 0) {
            dstWidth++;
        }
        if ((dstHeight & 0x01) > 0) {
            dstHeight++;
        }
        try {
            sfhCamera.startGetImgFrame();
        } catch (Exception e) {
            Log.e(TAG, "camera-1 " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    @Override
    public void onStop() {
        SharedPreferences agPreferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = agPreferences.edit();
        editor.putInt("ImgProcessTh", m_nImgProcessTh);
        editor.putInt("DotsDownTh", m_nPixDownDosTh);
        editor.putBoolean("BCMYKMode", m_bCMYKMode);
        editor.putBoolean("BInvertColor", m_bInvertColorMode);
        editor.putInt("CodeType", flag_DecodeMode);
        editor.apply();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void closeCamera() {
        sfhCamera.cancelFocus();
        flag_IsGetImg = false;
        sfhCamera.releaseCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            sfhCamera.cancelFocus();
            flag_IsGetImg = false;
            new AlertDialog.Builder(this).setTitle("EXIT?")
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    MyApplication mApp = (MyApplication) getApplication();
                                    mApp.setExit(true);
                                    finish();
                                }
                            })
                    .setNegativeButton("CANCEL",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Start();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface arg0) {
                            Log.i("*", "cancel is clicked");
                            Start();
                        }
                    }).show();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onRestart() {
        Log.i(TAG, "onRestart");
        super.onRestart();
        MyApplication mApp = (MyApplication) getApplication();
        if (mApp.isExit()) {
            closeCamera();
            finish();
        }
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "run");
            boolean flag = true;
            while (flag) {
                if (sfhCamera.checkCameraIsOK()) {
                    flag = false;
                }
            }
            flag_IsGetImg = true;
            sfhCamera.AutoFocusAndPreviewCallback();
            width = sfhCamera.mPreviewSize.width == 0 ? 480 : sfhCamera.mPreviewSize.width;
            height = sfhCamera.mPreviewSize.height == 0 ? 320 : sfhCamera.mPreviewSize.height;
            startGetImgFrame();
        } catch (Exception e) {
            Log.e(TAG, "camera-1 " + e.getMessage());
        }
    }

    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    class DecodeThread extends Thread {
        @Override
        public void run() {
            Log.i(TAG, "DecodeThread");
            source = new PlanarYUVLuminanceSource(
                    tempData, width, height, dstLeft, dstTop, dstWidth, dstHeight);
            BinaryBitmap bitmap2 = new BinaryBitmap(new HybridBinarizer(source));
            mBitmap = source.renderCroppedGreyscaleBitmap();

            String retString = "";
            //now add my code to replace original decode process using .so
            try {
                retString = DecodeUtils.decodeBitMap(bitmap2, mBitmap);

            } catch (NotFoundException | ReedSolomonException e) {
                e.printStackTrace();
            }

            Log.i(TAG, "result " + retString);

            if (retString.length()-retString.replace(" ","").length() > 14) {
                //add to avoid extra out of size
                Matrix matrix = new Matrix();
                matrix.preRotate(90);
                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(),
                        mBitmap.getHeight(), matrix, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();

                Intent intent = new Intent(DecodeActivity.this, ScanResultActivity.class);
                intent.putExtra("bmp", bytes);
                intent.putExtra("number", retString);
                startActivity(intent);
            }
        }
    }

}
