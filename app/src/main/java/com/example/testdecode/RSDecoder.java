package com.example.testdecode;

/**
 * Created by Ruiming Huang on 2016/5/27.
 */
public class RSDecoder {
    //now is coming the ndk and c code
    static { //载入名为“lb”的C++库
        System.loadLibrary("lb");
    }

    public native int addInt(int a, int b);

    public native int decodeRS(int[] input, int[] ret);
}
