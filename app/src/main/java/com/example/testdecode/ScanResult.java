package com.example.testdecode;



import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;

import android.text.method.ScrollingMovementMethod;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import foster.src.other.ProgressView;



public class ScanResult extends Activity  implements OnClickListener
{
	private ImageView imgShow;
    private TextView txtType;
    private TextView txtTime;
	private TextView txtR;
	public TextView txtR2;
	private Button btnBack;
	private Button btnCopy;
	private Button btnWebcheck;
	private Button btnMsgcheck;
	ProgressView pv=null;
	
	private int flag_DecodeMode=0;
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		setContentView(R.layout.scanresult);
		
		imgShow=(ImageView)findViewById(R.id.imgShow);
		txtType=(TextView)findViewById(R.id.txtType);
		txtR=(TextView)findViewById(R.id.txtRst);
		txtR2=(TextView)findViewById(R.id.txtRst2);
		txtTime=(TextView)findViewById(R.id.txtTime);
		btnBack=(Button)findViewById(R.id.btnBack);
		btnBack.setOnClickListener(this);


		//monospace display
		txtR.setTextScaleX(1.5f);

		txtR.setTextSize(11);

		txtR.setTypeface(Typeface.MONOSPACE);
		txtR2.setTextScaleX(1.5f);

		txtR2.setTextSize(11);

		txtR2.setTypeface(Typeface.MONOSPACE);
/*		btnCopy=(Button)findViewById(R.id.btnCopy);
		btnCopy.setOnClickListener(this);
		btnWebcheck=(Button)findViewById(R.id.btnWebcheck);
		btnWebcheck.setOnClickListener(this);
		btnMsgcheck=(Button)findViewById(R.id.btnMsgcheck);
		btnMsgcheck.setOnClickListener(this);*/

		txtR2.setMovementMethod(ScrollingMovementMethod.getInstance());
		
		String ret=getIntent().getStringExtra("number");
		int num=12-ret.length();
		if(num>0)
		{
			for(int i=0;i<num;i++)
			{
				ret="0"+ret;
			}
		}
		txtR.setText(ret);
		
		flag_DecodeMode=getIntent().getExtras().getInt("type");
		switch(flag_DecodeMode)
		{
		   case 0:
			   txtType.setText("          Screen Code");
			   break;

		    default:
		        txtType.setText("          Screen Code");
				break;
		}
		
		txtTime.setText("          "+getIntent().getStringExtra("time"));
		
		if(getIntent().getExtras().getInt("flag")==0)
		{
			txtR2.setText(getIntent().getStringExtra("result").toString());
		}
/*		if(getIntent().getExtras().getInt("flag")==1)
		{
			Bitmap bmp=(Bitmap)getIntent().getExtras().get("bmp"); 
			imgShow.setAdjustViewBounds(true);
			imgShow.setMaxHeight(100);
			imgShow.setMaxWidth(100);
			imgShow.setImageBitmap(bmp);
		}*/
		else
		{
			//add to avoid exceeding the extra size limit
			byte[] bytes = getIntent().getByteArrayExtra("bmp");
			Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			//
//			Bitmap bmp=(Bitmap)getIntent().getExtras().get("bmp");
			imgShow.setImageBitmap(bmp);
		}
	}

    @Override 
    public void onClick(View view)
    {
    	Intent intent=null;
    	switch(view.getId())
    	{
    	case R.id.btnBack:
    		intent=new Intent(this,testdecode.class);
    		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    		startActivity(intent);
    		finish();
    		break;

    	}
    }
    
}
