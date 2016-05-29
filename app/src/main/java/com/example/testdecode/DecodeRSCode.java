package com.example.testdecode;

import android.util.Log;

/**
 * Created by Ruiming Huang on 2016/5/28.
 */
public class DecodeRSCode {
    static{
        try{
            // 此处即.so文件的绝对路径
            System.loadLibrary("decoders");
        } catch(UnsatisfiedLinkError e)
        {
            Log.e("DecodeRSCode", "Cannot load library:\n " +
                    e.toString() );
        }
    }

    // 第一个参数为纠错码，第二个为纠错过后的数据码字
    // 返回值若非负，说明解码成功
    // 若返回值为-1，说明说明错误过多无法修正
    public static native int decode_rs_code(int[] input, int[] ret);

    public int[] RsDecode(int[] input){
        // 获取2组(12,8)的RS码
        // 纠错码长度=2组 * 12个码字/组 = 24
//        int[] input = {14, 12, 1, 2, 3, 9, 6, 3, 8, 9, 10, 7, 10, 6, 10, 13, 7, 10, 12, 1, 10, 1, 1, 9};
        // 数据码字长度=2组 * 8个数据码字/组 = 16;
        int[] ret = new int[2 * 8];
        int type = decode_rs_code(input,ret);
        if (type >= 0){
            for (int i = 0;i < ret.length;i++){
                System.out.printf(ret[i] + " ");
            }
            System.out.println(":thecode");
            return ret;
        }
        else{
            System.out.println("Decode Failed");
        }

        return null;
    }
}
