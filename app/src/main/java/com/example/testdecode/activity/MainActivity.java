package com.example.testdecode.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.testdecode.MyApplication;
import com.example.testdecode.R;

public class MainActivity extends Activity {
    private final int SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        StartAnimations();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setParament();
                Intent intent = new Intent(MainActivity.this, DecodeActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void StartAnimations() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.translate);
        anim.reset();
        ImageView iv = (ImageView) findViewById(R.id.splashscreen);
        iv.clearAnimation();
        iv.startAnimation(anim);

    }


    protected void onRestart() {
        super.onRestart();
        MyApplication mApp = (MyApplication) getApplication();
        if (mApp.isExit()) {
            finish();
        }
    }

    private void setParament() {
        MyApplication mApp = (MyApplication) getApplication();

        FeatureInfo[] feature = this.getPackageManager().getSystemAvailableFeatures();
        for (FeatureInfo featureInfo : feature) {
            if (PackageManager.FEATURE_CAMERA_FLASH.equals(featureInfo.name)) {
                mApp.setFlashlightBool(true);
                break;
            }
        }
    }
}




