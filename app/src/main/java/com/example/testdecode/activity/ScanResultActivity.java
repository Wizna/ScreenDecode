package com.example.testdecode.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.testdecode.R;

public class ScanResultActivity extends Activity {
    private ImageView imgShow;
    private EditText txtR;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.scanresult);

        imgShow = (ImageView) findViewById(R.id.imgShow);
        txtR = (EditText) findViewById(R.id.txtRst);
        txtR.setFocusable(false);
        String ret = getIntent().getStringExtra("number");
        txtR.setText(ret);

        byte[] bytes = getIntent().getByteArrayExtra("bmp");
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imgShow.setImageBitmap(bmp);
    }

}
