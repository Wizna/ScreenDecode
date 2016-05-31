package com.example.testdecode;

import android.app.Application;


public class MyApplication extends Application {

    private static boolean isProgramExit = false;
    private static boolean isHaveFlashlight = false;
    private static boolean isHaveSDcard = true;

    public boolean isExit() {
        return isProgramExit;
    }

    public void setExit(boolean exit) {
        isProgramExit = exit;
    }

    public boolean getFlashlightBool() {
        return isHaveFlashlight;
    }

    public void setFlashlightBool(boolean exit) {
        isHaveFlashlight = exit;
    }

    public boolean getSDcardBool() {
        return isHaveSDcard;
    }

    public void setSDcardBool(boolean exit) {
        isHaveSDcard = exit;
    }
}
