package com.example.testdecode;

import android.app.Application;


public class MyApplication extends Application {

    private static boolean isProgramExit = false;

    public void setExit(boolean exit) {
        isProgramExit = exit;
    }

    public boolean isExit() {
        return isProgramExit;
    }


    private static boolean isHaveFlashlight = false;

    public void setFlashlightBool(boolean exit) {
        isHaveFlashlight = exit;
    }

    public boolean getFlashlightBool() {
        return isHaveFlashlight;
    }


    private static boolean isHaveSDcard = true;

    public void setSDcardBool(boolean exit) {
        isHaveSDcard = exit;
    }

    public boolean getSDcardBool() {
        return isHaveSDcard;
    }
}
