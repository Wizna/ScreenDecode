package com.example.testdecode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.lang.Thread;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import com.google.zxing.Android.PlanarYUVLuminanceSource;
import com.google.zxing.Android.RGBLuminanceSource;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.PerspectiveTransform;

import foster.src.other.ProgressView;
//import com.example.testdecode.Decoding;
//import foster.scan.SCDecode;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Matrix;
import android.hardware.Camera;

import android.media.AudioManager;
import android.media.ImageReader;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.content.SharedPreferences;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;




public class testdecode extends Activity implements OnClickListener, Runnable
{
	/** Called when the activity is first created. */
	boolean whetherCatch=false;
	private final String PREFERENCES_NAME = "decodesetting";
	
	private SurfaceView sfvCamera;
	private SFHCamera sfhCamera;
	private ImageView imgView;
	private Button btn01;
	
	int width;
	int height;
	//int dstLeft, dstTop, dstWidth, dstHeight;
	int dstLeft = 0;
	int dstTop = 0;
	int dstWidth = 0;
	int dstHeight = 0;
	int screenWidth;
	int screenHeight;
	private int m_nImgProcessTh;
	private int m_nPixDownDosTh;
	private boolean m_bCMYKMode;
	private boolean m_bInvertColorMode;
		
	private LinearLayout literCenter;
	private LinearLayout literImg;
	
	private int flag_DecodeMode=0;


	public boolean flag_light=false;
	private boolean flag_IsGetImg=true;
//	ImageScanner scanner;
	
	ProgressView pv=null;
	
	AudioManager audioManager;
	
	byte[] myYUV;
	long retNumber=0;
	//String strResultCode;
	String strxmlResult;
//	Decoding dc = null;
//	SCDecode dc = null;
	Bitmap mBitmap;
	PlanarYUVLuminanceSource source;
	byte[] tempData;
	boolean bClickBtn = false;
	Thread t1;
	private DecodeThread myDecodThread;

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		int nCMYKMode = 0;
    	int nInvertColorMode = 0;
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        setContentView(R.layout.testdecode);

        audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
 /*       Decoding dc = new Decoding();
        long retNumber = dc.decodImg(bmp);*/

		DisplayMetrics  dm =new DisplayMetrics(); 
        getWindowManager().getDefaultDisplay().getMetrics(dm);  
        screenWidth=dm.widthPixels;
        screenHeight=dm.heightPixels;
        Log.i("screen wid", String.valueOf(screenWidth));
        Log.i("screen hei", String.valueOf(screenHeight));
		
          
        literCenter=(LinearLayout)findViewById(R.id.literCenter);
        literImg=(LinearLayout)findViewById(R.id.literImg);
			
		imgView = (ImageView) this.findViewById(R.id.ImageView01);
		sfvCamera = (SurfaceView) this.findViewById(R.id.sfvCamera);
		btn01=(Button) this.findViewById(R.id.btn01);
		btn01.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				whetherCatch=!whetherCatch;
			}
		});
		sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight,previewCallback, errorCallback);
		sfhCamera.parentActivity=this;
		
		SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		m_nImgProcessTh= preferences.getInt("ImgProcessTh",6);
		m_nPixDownDosTh = preferences.getInt("DotsDownTh",4);
	    m_bCMYKMode = preferences.getBoolean("BCMYKMode",false);
		m_bInvertColorMode = preferences.getBoolean("BInvertColor", false);
		flag_DecodeMode = preferences.getInt("CodeType", 0);

//		if(dc == null)
//		{
////			dc=new Decoding();
//			dc = new SCDecode();
//		}
//		dc.SetDefaultParamete(0);
		t1=new Thread(this);
		myDecodThread=new DecodeThread();
		
	}
	
	
    public void Start()
    {
    	Log.i("start()","...................");
    	if (t1.getState().equals(Thread.State.NEW))
    	{
    		t1.start();
    	}else if (t1.getState().equals(Thread.State.TERMINATED))
    	{
    		t1.run();
    	}
    	
    }
    
	public void startGetImgFrame()
	{	
	//	if(dstWidth==0)
		{
			Log.i("*", "startGetImgFrame");
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			dstLeft = (literCenter.getLeft()+imgView.getLeft()) * width/ metrics.widthPixels;
			dstTop = (literImg.getTop()+imgView.getTop()) * height/ metrics.heightPixels;
			dstWidth = (imgView.getRight() - imgView.getLeft())* width/metrics.widthPixels;;
			dstHeight = (imgView.getBottom() - imgView.getTop())* height/metrics.heightPixels;
			Log.i("process","processing...");
			while((dstLeft&0x03) > 0)
	        {
				dstLeft++;
	        }
	        if((dstTop & 0x01)>0)
	        {
	            dstTop++;
	        }
	        while((dstWidth & 0x03)>0)
	        {
	            dstWidth++;
	        }
	        if((dstHeight & 0x01)>0)
	        {
	        	dstHeight++;
	        }
		}
		try{
			sfhCamera.startGetImgFrame();
		}catch(Exception e){
			Log.e("camera-1", e.getMessage());
		}
	}
	
	private Camera.ErrorCallback errorCallback = new Camera.ErrorCallback() 
	{
		@Override
		public void onError(int error, Camera camera) 
		{
			try
			{
				//camera.stopPreview();
				//camera.release();
				sfhCamera=null;
				sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight,previewCallback, errorCallback);
				//sfhCamera.createCamera(PixelFormat.YCbCr_422_SP);
				sfhCamera.createCamera(ImageFormat.NV21);
				Log.i("callback", "Here is the error call back");
			}
			catch (Exception ex)
			{
				Log.i("aa", ex.getMessage().toString());
			}
		}
	};
	

	private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() 
	{
		@Override
		public void onPreviewFrame(byte[] data, Camera arg1)
		{
			Log.i("PreviewCallback","PreviewCallback");
			try{
				
				if(data!=null)
				{			
					try
					{
				    	if (myDecodThread.getState().equals(Thread.State.NEW))
				    	{
				    		tempData = data;
				    		myDecodThread.start();
				    	}
				    	else if (myDecodThread.getState().equals(Thread.State.TERMINATED))
				    	{
				    		if((retNumber==0 || retNumber==-1) && !bClickBtn)
				    		{
					    		tempData = data;
					    		myDecodThread.run();
				    		}
				    	}

						if(flag_IsGetImg)
						{
							startGetImgFrame();
						}
					}
					catch(Exception ex)
					{
						Log.e("decode", ex.getMessage());
					}
				}
			  }
			  catch(Exception e)
			  {
				Log.e("camera-13", e.getMessage());
			  }
		}
	};
 
    @Override 
    public void onClick(View view)
    {
    	//myDecodThread.suspend();
    	bClickBtn = true;
    	Intent intent=null;
    	bClickBtn = false;
    }

    @Override
    protected void onPause()
    {
    	super.onPause();
    	closeCamera();
    }
    @Override
    public void onStop() 
    {
       
        SharedPreferences agPreferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = agPreferences.edit();
    	editor.putInt("ImgProcessTh",m_nImgProcessTh);
	    editor.putInt("DotsDownTh",m_nPixDownDosTh);
	    editor.putBoolean("BCMYKMode",m_bCMYKMode);
		editor.putBoolean("BInvertColor", m_bInvertColorMode);
		editor.putInt("CodeType", flag_DecodeMode);
        editor.commit();
        super.onStop();
    }  	     

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid()); 
    }
    

	private void closeCamera()
	{
		Log.i("Camera","cacenl click");
		sfhCamera.cancelFocus();
		flag_IsGetImg=false;
		sfhCamera.releaseCamera(); 
	}

    @Override  
    public boolean onKeyDown(int keyCode,KeyEvent event) 
    {  
        if (keyCode == KeyEvent.KEYCODE_BACK&& event.getRepeatCount() == 0) 
        {  
        	Log.i("*", " enter");
        	sfhCamera.cancelFocus();
        	flag_IsGetImg=false;
        	new AlertDialog.Builder(this).setTitle(
        			"EXIT?").setPositiveButton("OK",
        			new DialogInterface.OnClickListener()
        			{
        				
        				public void onClick(DialogInterface dialog, int whichButton)
        				{
        			         MyApplication mApp = (MyApplication)getApplication(); 
        			         mApp.setExit(true); 
        			         finish(); 
        				}
        			}).setNegativeButton("CANCEL",
        			new DialogInterface.OnClickListener()
        			{
        				public void onClick(DialogInterface dialog, int whichButton)
        				{
        					Start();
        				}
        			}).setOnCancelListener(new DialogInterface.OnCancelListener() {
						
						@Override
						public void onCancel(DialogInterface arg0) {
							Log.i("*", "cancel am clicked");
							Start();
							// TODO Auto-generated method stub
							
						}
					}).show();
            return true;  
        } 
        else if(keyCode ==KeyEvent.KEYCODE_VOLUME_UP)
        {
        	   audioManager.adjustStreamVolume(
        	           AudioManager.STREAM_MUSIC,
        	           AudioManager.ADJUST_RAISE,
        	           AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
        	       return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
       	   audioManager.adjustStreamVolume(
    	           AudioManager.STREAM_MUSIC,
    	           AudioManager.ADJUST_LOWER,
    	           AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
    	       return true;
        }
        else    
        {
        	return super.onKeyDown(keyCode,event);  
        }
    }
    
	
    @Override 
    protected void onRestart() 
    { 
    	Log.i("*", "onRestart");
    	super.onRestart(); 
    	retNumber=0;
        MyApplication mApp = (MyApplication)getApplication(); 
        if(mApp.isExit()) 
        { 
           closeCamera();
           finish();
        }
    }    

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
        try 
        {
        	Log.i("*", "run");
    		boolean flag=true;
            while(flag)
            {
            	if(sfhCamera.checkCameraIsOK())
            	{
            		flag=false;
            	}
            }
            flag_IsGetImg=true;
            sfhCamera.AutoFocusAndPreviewCallback();
    		width=sfhCamera.mPreviewSize.width==0?480:sfhCamera.mPreviewSize.width;
    		height=sfhCamera.mPreviewSize.height==0?320:sfhCamera.mPreviewSize.height;
            
    		Log.i("get width is:", String.valueOf(width));
    		Log.i("get height is:", String.valueOf(height));
            startGetImgFrame();
        } 
        catch (Exception e) 
        {
            Log.e("camera-1", e.getMessage());
        }

	}
	public static boolean isNumeric(String str)
	{   
	    Pattern pattern = Pattern.compile("[0-9]*");   
	    return pattern.matcher(str).matches();      
	 } 
	class DecodeThread extends Thread {
		@Override
		public void run() {
			
			Log.i("*", "DecodeThread");
			source = new PlanarYUVLuminanceSource(
					tempData, width, height, dstLeft, dstTop, dstWidth, dstHeight);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			mBitmap = source.renderCroppedGreyscaleBitmap();
			myYUV=source.getTheYUV();
//			if(dc == null)
//			{
////				    dc=new Decoding();
//				dc = new SCDecode();
//			}

			retNumber=0;
			String retString="";
//			retNumber=dc.decodeBitmap(mBitmap);
//				retNumber=dc.decodeYUV(myYUV,dstWidth,dstHeight,flag_DecodeMode);
			//now add my code to replace original decode process using .so
			try {
//				retNumber = decodeBitMap(bitmap,mBitmap);
				retString = decodeBitMap(bitmap,mBitmap);

			} catch (NotFoundException e) {
				e.printStackTrace();
			}
			//

//			Log.i("result", String.valueOf(retNumber));
			Log.i("result", retString);


//			if(retNumber!=0&&retNumber!=-1)
				if(retString.length()>5&&retString.contains("\n"))

				{
				//now we get the net string and play the multimeida data, so we need not show the decode result activity
			   //we add these codes to get media data and play
				String ret= String.valueOf(retNumber);
				int num=12-ret.length();
				if(num>0)
				{
					for(int i=0;i<num;i++)
					{
						ret="0"+ret;
					}
				}

				//add to avoid extra out of size
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte[] bytes = stream.toByteArray();
				//

				Intent intent=new Intent(testdecode.this,ScanResult.class);
				intent.putExtra("flag", 2);
				intent.putExtra("type", flag_DecodeMode);
				Date now=new Date(System.currentTimeMillis());
				intent.putExtra("time", now.toString());
//					intent.putExtra("bmp", mBitmap);
				intent.putExtra("bmp", bytes);
				intent.putExtra("number", retString);
//					intent.putExtra("number", String.valueOf(retNumber));

					startActivity(intent);

			}
		}
	}
	
	public void onResume()
    {
		Log.i("*", "onResume");
        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        int ii = 0;
    }

	public String decodeBitMap(BinaryBitmap bitmap,Bitmap grayMap) throws NotFoundException {
		//no result, then return "0"
		int result= 0;
        boolean[][] whetherChecked=new boolean[height][width];
        boolean[][] bitsBool=new boolean[height][width];
        float[][][] points=new float[20][20][2];
        int indexI=0;
        int indexJ=0;
        boolean alreadyOne=false;
        boolean lineChange=false;



		//now add find direction and chose edge line
		ArrayList<Point> pointArrayList=new ArrayList<>();

        if(whetherCatch) {
			whetherCatch=false;

			//try zxing binarization
			BitMatrix binaryMatrix=bitmap.getBlackMatrix();

			for(int i=0;i<binaryMatrix.width;i++){
				String str="";
				for(int j=0;j<binaryMatrix.height;j++){
					if(!binaryMatrix.get(i,j)) {
//						str = grayMap.getPixel(i,j)+"|"+str;
						grayMap.setPixel(i,j,0xff000000);
						bitsBool[grayMap.getHeight()-1-j][i] = false;
					}
					else {
//						str = grayMap.getPixel(i,j)+"|"+str;
						grayMap.setPixel(i,j,0xffffffff);
						bitsBool[grayMap.getHeight()-1-j][i] = true;
					}
//					System.out.print(binaryMatrix.get(i,j));
				}
//				Log.w("poi",str);

//				System.out.println();
			}

//			if (true)
//				return "000000000\n";

			//now find binarization boolean for hough
			int lowerBound=200;
			int higherBound=50;
//			for(int i=0;i<grayMap.getWidth();i++){
//				for(int j=0;j<grayMap.getHeight();j++){
//					int temp=grayMap.getPixel(i,j);
//					int transTemp=(temp&0xff);
//					if(transTemp>higherBound)
//						higherBound=transTemp;
//
//					if(transTemp<lowerBound)
//						lowerBound=transTemp;
//				}
//			}
//
//			//need to reuse later
//			for(int i=0;i<grayMap.getWidth();i++){
//				String str="";
//				for(int j=0;j<grayMap.getHeight();j++){
//					//can use getPixels to optimaize
//					int temp=grayMap.getPixel(i,j);
//					int transTemp=(temp&0xff);
//					if(transTemp>(lowerBound+higherBound)/2) {
//						str = "_"+str;
////						grayMap.setPixel(i,j,0);
//						bitsBool[grayMap.getHeight()-1-j][i] = false;
//					}
//					else {
//						str = "*"+str;
////						grayMap.setPixel(i,j,255);
//						bitsBool[grayMap.getHeight()-1-j][i] = true;
//					}
//				}
//				Log.w("poi",str);
//			}
			Log.w("before rotate ",""+grayMap.getWidth()+"|"+grayMap.getHeight());


			//now add rotate and find standard
//			ArrayData inputData = getArrayDataFromImage(grayMap);
//			int minContrast =5;// (args.length >= 4) ? 64 : Integer.parseInt(args[4]);
//			ArrayData outputData = houghTransform(inputData, 180, (int)Math.hypot(inputData.width,inputData.height), minContrast,bitsBool);
//
//			//print result
//			int best=0;
//			for(int i=0;i<outputData.width;i++){
//				int tempresult=0;
//				boolean tempStart=false;
//				boolean tempEnd=false;
//				for(int j=0;j<outputData.height;j++){
//					if(!tempStart&&outputData.get(i,j)!=0)
//						tempStart=true;
//					if(tempStart&&!tempEnd&&outputData.get(i,j)==0)
//						tempresult++;
//
//					if(tempStart&&!tempEnd&&outputData.get(i,j)==0&&(j+1)<outputData.height
//							&&outputData.get(i,j+1)==0&&(j+2)<outputData.height&&outputData.get(i,j+2)==0)
//						tempEnd=true;
//					System.out.print(outputData.get(i,j)+"|");
//				}
//				if(tempresult>best) {
//					best = tempresult;
//					bestTheta=i;
//				}
//			System.out.println(i+":"+tempresult);
////			System.out.println();
//			}
//			System.out.println("best:"+bestTheta+"|"+best);
//			List<Double> doubleList=new ArrayList<>();
//			for(int i=2;i<outputData.height-2;i++){
//				if(outputData.get(bestTheta, i)>outputData.get(bestTheta,i-1)&&outputData.get(bestTheta,i)>outputData.get(bestTheta,i+1)) {
////				System.out.println(i + ":" + (double) outputData.get(bestTheta, i) / (double) (outputData.get(bestTheta, i - 1) + outputData.get(bestTheta, i)+outputData.get(bestTheta, i + 1)));
//					doubleList.add((double) outputData.get(bestTheta, i) / (double) (outputData.get(bestTheta, i - 1) +outputData.get(bestTheta, i)+ outputData.get(bestTheta, i + 1)));
//
//				}
//				else {
////				System.out.println(i+":"+0.0);
//				}
//			}
//
//			double[] doubles=new double[6];
//			int[] ints=new int[6];
//			for(int i=0;i<doubleList.size();i++){
//				doubles[i%6]+=doubleList.get(i);
//				ints[i%6]++;
//			}
//
//			int standardIndex=0;
//			double standardRate=0;
//			for(int i=0;i<6;i++){
//				System.out.println(i+":"+doubles[i]/ints[i]);
//				if(doubles[i]/ints[i]>standardRate){
//					standardIndex=i;
//					standardRate=doubles[i]/ints[i];
//				}
//			}
//
//			System.out.println("size:"+doubleList.size());
//			int finalStandard=(doubleList.size()%6+6-standardIndex-1)%6;
//			System.out.println("standard:" + finalStandard);
//
//			Log.w("best theta",bestTheta+"|"+finalStandard);



//			Matrix tempTransMatrix = new Matrix();
//			tempTransMatrix.postRotate(bestTheta-90);
//			grayMap= Bitmap.createBitmap(grayMap, 0, 0, grayMap.getWidth(), grayMap.getHeight(), tempTransMatrix, true);
//			Log.w("after rotate ",""+grayMap.getWidth()+"|"+grayMap.getHeight());
//
//			//end of rotate
//
//			Log.w("height and width:",grayMap.getHeight()+"|"+grayMap.getWidth());
//
//			//need to reuse later
//            for(int i=0;i<grayMap.getWidth();i++){
//                String str="";
//				for(int j=0;j<grayMap.getHeight();j++){
//					//can use getPixels to optimaize
//                    int temp=grayMap.getPixel(i,j);
//                    int transTemp=(temp&0xff);
//                    if(transTemp>(lowerBound+higherBound)/2||(transTemp==0)) {
//                        str = "_"+str;
//                        bitsBool[grayMap.getHeight()-1-j][i] = false;
//                    }
//                    else {
//                        str = "*"+str;
//                        bitsBool[grayMap.getHeight()-1-j][i] = true;
//
//                    }
//                }
//                Log.w("poi",str);
//            }

			//another way to find theta, with finding nearest points and extend
			for (int j = 0; j < width; j++) {
				for (int i = 0; i < height; i++) {
//                    Log.w("d","dddddd");
					if(whetherChecked[i][j]){
						lineChange=true;
						alreadyOne=true;
					}else
					if(bitsBool[i][j]){
//                        whetherChecked[i][j]=true;

						int originX=j;
						int originY=i;

						int changeX=j;
						int changeY=i;

						int lastMove=1;

						int xs=j,xe=j,ys=i,ye=i;

						while(true){
//                            Log.w("pos:",""+changeX+"|"+changeY+"|"+originX+"|"+originY+"|"+lastMove);
//                            int thisMove=(lastMove+7)/4;
							if(lastMove==0){
								if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
									changeY--;
									lastMove = 3;
								}else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
									changeX++;
									lastMove = 0;
								}else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
									changeY++;
									lastMove = 1;
								}else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
									changeX--;
									lastMove = 2;
								}
							}else if(lastMove==1){
								if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
									changeX++;
									lastMove = 0;
								}else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
									changeY++;
									lastMove = 1;
								}else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
									changeX--;
									lastMove = 2;
								}else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
									changeY--;
									lastMove = 3;
								}
							}else if(lastMove==2){
								if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
									changeY++;
									lastMove = 1;
								}else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
									changeX--;
									lastMove = 2;
								}else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
									changeY--;
									lastMove = 3;
								}else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
									changeX++;
									lastMove = 0;
								}
							}else if(lastMove==3){
								if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
									changeX--;
									lastMove = 2;
								}else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
									changeY--;
									lastMove = 3;
								}else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
									changeX++;
									lastMove = 0;
								}else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
									changeY++;
									lastMove = 1;
								}
							}

							if(changeX>xe)
								xe=changeX;

							if(changeX<xs)
								xs=changeX;

							if(changeY>ye)
								ye=changeY;

							if(changeY<ys)
								ys=changeY;

							if(changeX==originX&&changeY==originY)
								break;
						}



//						Log.w("loc","here|"+xs+"|"+xe+"|"+ys+"|"+ye);
						for(int setI=ys;setI<=ye;setI++){
							for(int setJ=xs;setJ<=xe;setJ++){
								whetherChecked[setI][setJ] = true;
							}
						}

//						System.out.println("loc1");
						float centX = (xs+xe)/2;
						float centY = (ys+ye)/2;

						pointArrayList.add(new Point(centX,centY));
//						System.out.println("loc2");

					}
				}

			}

			//the new way of finding theta
			int[] countTheta=new int[18];
			double foundTheta=0;
			boolean whetherFound = false;
			int countSelectingPoint=0;
			while (!whetherFound&&countSelectingPoint<pointArrayList.size()) {

				Point midPoint = pointArrayList.get(countSelectingPoint);
				countSelectingPoint++;

				for (int i = 0; i < pointArrayList.size(); i++) {
					pointArrayList.get(i).setDistanceTo(midPoint);
				}
				Collections.sort(pointArrayList);

				double[][] fourPoints = new double[4][2];
				double addDistance = 0;
				double addCrossDis = 0;
				for (int i = 1; i < 5; i++) {
					fourPoints[i - 1][0] = pointArrayList.get(i).getCentX();
					fourPoints[i - 1][1] = pointArrayList.get(i).getCentY();
//                    System.out.println("dis:" + pointArrayList.get(i).distance);
					addDistance += pointArrayList.get(i).distance;
				}

				int m=0, n=0;
				for(int i=0;i<6;i++){
					switch (i){
						case 0:m=0;n=1;break;
						case 1:m=0;n=2;break;
						case 2:m=0;n=3;break;
						case 3:m=1;n=2;break;
						case 4:m=1;n=3;break;
						case 5:m=2;n=3;break;
					}

					double disPair = pointArrayList.get(m+1).distance + pointArrayList.get(n + 1).distance;
					double ratePair = (pointArrayList.get(m+1).distance / pointArrayList.get(n + 1).distance);
					Point[] possiblePoints = new Point[2];
					if (1.053 > ratePair && ratePair > 0.95 && Math.hypot((fourPoints[m][1] - fourPoints[n][1]), (fourPoints[m][0] - fourPoints[n][0])) / disPair > 0.95) {
						possiblePoints[0] = new Point((2 * pointArrayList.get(m+1).centX - midPoint.centX), (2 * pointArrayList.get(m+1).centY - midPoint.centY));
						possiblePoints[1] = new Point((2 * pointArrayList.get(n+1).centX - midPoint.centX), (2 * pointArrayList.get(n+1).centY - midPoint.centY));

						int findResult = findLinePoint(possiblePoints, pointArrayList);
						if (findResult != -1){
							whetherFound = true;

							foundTheta=Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180;
							System.out.println(Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180);
							break;
						}
					}
				}
//				for (int i = 0; i < 3; i++) {
//					if (i == 0) {
//						m = 1;
//						n = 2;
//					} else if (i == 1) {
//						m = 0;
//						n = 2;
//					} else {
//						m = 0;
//						n = 1;
//					}
//					if (linesIntersect(fourPoints[i][0], fourPoints[i][1], fourPoints[3][0], fourPoints[3][1],
//							fourPoints[m][0], fourPoints[m][1], fourPoints[n][0], fourPoints[n][1])) {
//						double disPair = pointArrayList.get(4).distance + pointArrayList.get(i + 1).distance;
//						double ratePair = (pointArrayList.get(4).distance / pointArrayList.get(i + 1).distance);
//						Point[] possiblePoints = new Point[4];
//						if (1.053 > ratePair && ratePair > 0.95 && Math.hypot((fourPoints[3][1] - fourPoints[i][1]), (fourPoints[3][0] - fourPoints[i][0])) / disPair > 0.95) {
//							possiblePoints[0] = new Point((2 * pointArrayList.get(4).centX - midPoint.centX), (2 * pointArrayList.get(4).centY - midPoint.centY));
//							possiblePoints[1] = new Point((2 * pointArrayList.get(i + 1).centX - midPoint.centX), (2 * pointArrayList.get(i + 1).centY - midPoint.centY));
//
//
//						} else {
//
//							disPair = pointArrayList.get(m + 1).distance + pointArrayList.get(n + 1).distance;
//							ratePair = (pointArrayList.get(m + 1).distance / pointArrayList.get(n + 1).distance);
//							if (1.053 > ratePair && ratePair > 0.95 && Math.hypot((fourPoints[m][1] - fourPoints[n][1]), (fourPoints[m][0] - fourPoints[n][0])) / disPair > 0.95) {
//								possiblePoints[2] = new Point((2 * pointArrayList.get(m + 1).centX - midPoint.centX), (2 * pointArrayList.get(m + 1).centY - midPoint.centY));
//								possiblePoints[3] = new Point((2 * pointArrayList.get(n + 1).centX - midPoint.centX), (2 * pointArrayList.get(n + 1).centY - midPoint.centY));
//
//							}
//						}
//
//						int findResult = findLinePoint(possiblePoints, pointArrayList);
//						if (findResult != -1){
//							whetherFound = true;
////                            System.out.println("find:" + findResult);
//
//
//							if (findResult < 2) {
//								foundTheta=Math.atan((fourPoints[3][1] - fourPoints[i][1]) / (fourPoints[3][0] - fourPoints[i][0])) / Math.PI * 180;
//								System.out.println(Math.atan((fourPoints[3][1] - fourPoints[i][1]) / (fourPoints[3][0] - fourPoints[i][0])) / Math.PI * 180);
//								countTheta[Math.atan((fourPoints[3][1] - fourPoints[i][1]) / (fourPoints[3][0] - fourPoints[i][0])) < 0 ? (int) (Math.atan((fourPoints[3][1] - fourPoints[i][1]) / (fourPoints[3][0] - fourPoints[i][0])) / Math.PI * 18 + 18) : (int) (Math.atan((fourPoints[3][1] - fourPoints[i][1]) / (fourPoints[3][0] - fourPoints[i][0])) / Math.PI * 18)]++;
////                                System.out.println(points[3][1] + "|" + points[3][0] + "|" + points[i][1] + "|" + points[i][0] + "|" + midPoint.centY + "|" + midPoint.centX);
//							} else {
//								foundTheta=Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180;
//								System.out.println(Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180);
////                                System.out.println(points[m][1] + "|" + points[m][0] + "|" + points[n][1] + "|" + points[n][0] + "|" + midPoint.centY + "|" + midPoint.centX);
//								countTheta[Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) < 0 ? (int) (Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 18 + 18) : (int) (Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 18)]++;
//
//							}
//							break;
//						}
//					}
//				}
			}

			if(!whetherFound)
				return "-1"+countSelectingPoint;



			//now trans binarization result in BinaryBitmap to bitmap

			//rotate
			Matrix tempTransMatrix = new Matrix();
			tempTransMatrix.postRotate((float)(foundTheta-90));
			grayMap= Bitmap.createBitmap(grayMap, 0, 0, grayMap.getWidth(), grayMap.getHeight(), tempTransMatrix, true);
			Log.w("after rotate",""+grayMap.getWidth()+"|"+grayMap.getHeight());
			//end of rotate

			//now start finding points after rotate
			//how new bitmap get into old width and height?
			for(int i=0;i<grayMap.getWidth();i++){
				String str="";
				for(int j=0;j<grayMap.getHeight();j++){
					//can use getPixels to optimaize
					int temp=grayMap.getPixel(i,j);
					int transTemp=(temp&0xff);
					if(transTemp==0) {
						str = "_"+str;
//						grayMap.setPixel(i,j,0);
						bitsBool[grayMap.getHeight()-1-j][i] = false;
					}
					else {
						str = "*"+str;
//						grayMap.setPixel(i,j,255);
						bitsBool[grayMap.getHeight()-1-j][i] = true;
					}
				}
				Log.w("poi",str);
			}

//			if(true)
//				return "000000000000\n";

			whetherChecked=new boolean[height][width];

//			int linesIndex=0;
			boolean firstRound=true;
			double interval=0.0;
            for (int j = 0; j < width; j++) {
                for (int i = 0; i < height; i++) {
//                    Log.w("d","dddddd");
					if(whetherChecked[i][j]){
						lineChange=true;
						alreadyOne=true;
					}else
                    if(bitsBool[i][j]){
//                        whetherChecked[i][j]=true;

                        int originX=j;
                        int originY=i;

                        int changeX=j;
                        int changeY=i;

                        int lastMove=1;

                        int xs=j,xe=j,ys=i,ye=i;

                        while(true){
//                            Log.w("pos:",""+changeX+"|"+changeY+"|"+originX+"|"+originY+"|"+lastMove);
//                            int thisMove=(lastMove+7)/4;
                            if(lastMove==0){
                                if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
                                    changeY--;
                                    lastMove = 3;
                                }else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
                                    changeX++;
                                    lastMove = 0;
                                }else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
                                    changeY++;
                                    lastMove = 1;
                                }else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
                                    changeX--;
                                    lastMove = 2;
                                }
                            }else if(lastMove==1){
                                if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
                                    changeX++;
                                    lastMove = 0;
                                }else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
                                    changeY++;
                                    lastMove = 1;
                                }else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
                                    changeX--;
                                    lastMove = 2;
                                }else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
                                    changeY--;
                                    lastMove = 3;
                                }
                            }else if(lastMove==2){
                                if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
                                    changeY++;
                                    lastMove = 1;
                                }else if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
                                    changeX--;
                                    lastMove = 2;
                                }else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
                                    changeY--;
                                    lastMove = 3;
                                }else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
                                    changeX++;
                                    lastMove = 0;
                                }
                            }else if(lastMove==3){
                                if((changeX-1>-1)&&!whetherChecked[changeY][changeX-1]&&bitsBool[changeY][changeX-1]){
                                    changeX--;
                                    lastMove = 2;
                                }else if((changeY-1>-1)&&!whetherChecked[changeY-1][changeX]&&bitsBool[changeY-1][changeX]){
                                    changeY--;
                                    lastMove = 3;
                                }else if((changeX+1<width)&&!whetherChecked[changeY][changeX+1]&&bitsBool[changeY][changeX+1]){
                                    changeX++;
                                    lastMove = 0;
                                }else if((changeY+1<height)&&!whetherChecked[changeY+1][changeX]&&bitsBool[changeY+1][changeX]){
                                    changeY++;
                                    lastMove = 1;
                                }
                            }

                            if(changeX>xe)
                                xe=changeX;

                            if(changeX<xs)
                                xs=changeX;

                            if(changeY>ye)
                                ye=changeY;

                            if(changeY<ys)
                                ys=changeY;

                            if(changeX==originX&&changeY==originY)
                                break;
                        }



                        Log.w("loc","here|"+xs+"|"+xe+"|"+ys+"|"+ye);
                        for(int setI=ys;setI<=ye;setI++){
                            for(int setJ=xs;setJ<=xe;setJ++){
                                whetherChecked[setI][setJ] = true;
                            }
                        }

                        float centX = (xs+xe)/2;
						float centY = (ys+ye)/2;

						//remove spots
						if(Math.abs(xe-xs)>=2&&Math.abs(ye-ys)>=2)
							pointArrayList.add(new Point(centX,centY));

						if(firstRound) {
							points[indexI][indexJ][0] = centX;
							points[indexI][indexJ][1] = centY;
						}else{
							if(centX-points[0][0][0]<0.3*interval){
								points[indexI][indexJ][0] = centX;
								points[indexI][indexJ][1] = centY;
							}else
							for(int m=0;m<(20-1);m++){
//								Log.w("index",""+m);
								if(Math.abs(centY-points[0][m][1])>Math.abs(centY-points[0][m+1][1])){
									if(points[0][m+1][1]==0f)
										Log.w("already"," exceed limit !!!!");
								}else{
//									if(){
//										//handle accident dot in not head line
//									}
									double tempIndexI=(centX-points[0][m][0])/interval;
//									Log.w("interval:",""+interval+";"+tempIndexI);
									double floorIndexI=Math.floor(tempIndexI);
									if(tempIndexI-floorIndexI>0.5)
										indexI=(int)floorIndexI+1;
									else
										indexI=(int)floorIndexI;

									if(points[indexI][m][0]==0f&&points[indexI][m][1]==0f){
										points[indexI][m][0] = centX;
										points[indexI][m][1] = centY;
									}else if(m==0&&centY>points[indexI][m][1]){
										points[indexI][m][0] = centX;
										points[indexI][m][1] = centY;
									}else if(m!=0){
										if(points[indexI-1][m][0]==0f&&points[indexI-1][m][1]==0f){
											points[indexI-1][m][0] = centX;
											points[indexI-1][m][1] = centY;
										}else if(points[indexI+1][m][0]==0f&&points[indexI+1][m][1]==0f){
											points[indexI+1][m][0] = centX;
											points[indexI+1][m][1] = centY;
										}
									}

									break;
								}
							}

						}
                        if(indexJ<19)
                        indexJ++;

                        lineChange=true;
                        alreadyOne=true;
                    }
                }
                if(!lineChange&&alreadyOne&&firstRound){
					firstRound=false;
					//sort
					sortPoints(points);
					Log.w("e","sort !!!!!!!!!!!!!!!!");

					//
					for(int i=0;i<20;i++){
						if(points[0][i][0]==0f&&points[0][i][1]==0f&&i>0){
							interval=(points[0][i-1][1]-points[0][0][1])/(double)(i-1);
							if(i<7){
								indexI=0;
								indexJ=0;
								firstRound=true;
								points=new float[20][20][2];
								break;
							}
							for(int k=1;k<i-1;k++){
								if(Math.abs(2*points[0][k][1]-points[0][k-1][1]-points[0][k+1][1])>3||Math.hypot(Math.abs(points[0][k+1][1]-points[0][k-1][1]),Math.abs(points[0][k+1][0]-points[0][k-1][0]))/(Math.hypot(Math.abs(points[0][k+1][1]-points[0][k][1]),Math.abs(points[0][k+1][0]-points[0][k][0]))+Math.hypot(Math.abs(points[0][k][1]-points[0][k-1][1]),Math.abs(points[0][k][0]-points[0][k-1][0])))<0.95){
									indexI=0;
									indexJ=0;
									firstRound=true;
									System.out.println("shrehold:"+Math.abs(2*points[0][k][1]-points[0][k-1][1]-points[0][k+1][1])+"|-|"+Math.hypot(Math.abs(points[0][k+1][1]-points[0][k-1][1]),Math.abs(points[0][k+1][0]-points[0][k-1][0]))/(Math.hypot(Math.abs(points[0][k+1][1]-points[0][k][1]),Math.abs(points[0][k+1][0]-points[0][k][0]))+Math.hypot(Math.abs(points[0][k][1]-points[0][k-1][1]),Math.abs(points[0][k][0]-points[0][k-1][0]))));
									points=new float[20][20][2];
									break;
								}
							}
							break;
						}
					}

//					linesIndex++;
                    if(indexI<19&&!firstRound) {

                        indexI++;
                        indexJ=0;
                    }

                    alreadyOne=false;
                }

                lineChange=false;

            }



//			if (true)
//				return "000000000000\n";
			sortPoints(points);


            for(int i=0;i<20;i++){
                String str="";
                for(int j=0;j<20;j++){
                    str+=formatString(""+(int)points[i][j][0],3);
					str+=formatString("|"+(int)points[i][j][1],7);
                }
                Log.w("points:",str);
            }

			//add perspective transform
			float[] xValues=new float[400];
			float[] yValues=new float[400];

			PerspectiveTransform perspectiveTransform=PerspectiveTransform.quadrilateralToQuadrilateral(
					points[0][0][0], points[0][0][1],
					points[0][6][0], points[0][6][1],
					points[6][0][0], points[6][0][1],
					points[6][6][0], points[6][6][1],
					50, 50,
					50, 330,
					330, 50,
					330, 330);

			for(int i=0;i<points.length;i++){
				for(int j=0;j<points.length;j++){
					xValues[i*20+j]=points[i][j][0];
					yValues[i*20+j]=points[i][j][1];
				}
			}

			perspectiveTransform.transformPoints(xValues,yValues);

			for(int i=0;i<points.length;i++){
				for(int j=0;j<points.length;j++){
					points[i][j][0]=xValues[i*20+j];
					points[i][j][1]=yValues[i*20+j];
				}
			}

			Log.w("","------------------------------------------------------------------------------");
			for(int i=0;i<20;i++){
				String str="";
				for(int j=0;j<20;j++){
					str+=formatString(""+points[i][j][0],5);
					str+=formatString("|"+points[i][j][1],5);
				}
				Log.w("points:",str);
			}
//			if(true)
//				return "-100000000000";

			//no actual use anymore
//			float[][][] tempPoints=new float[20][20][2];
//			int tempPointsIndex=0;
//			for(int i=0;i<20;i++){
//				if(points[i][6][0]!=0||points[i][6][1]!=0){
//					for(int j=0;j<20;j++){
//						tempPoints[tempPointsIndex][j][0]=points[i][j][0];
//						tempPoints[tempPointsIndex][j][1]=points[i][j][1];
//
//					}
//					tempPointsIndex++;
//				}
//			}
//
//			points=tempPoints;

			//now start process
//			float internalX = (points[0][6][1]-points[0][0][1])/6;//row
//			float internalY = (points[6][0][0]-points[0][0][0])/6;//shulie
//			float[] columnInternal=new float[6];
//			for(int i=0;i<6;i++){
//				columnInternal[i] = points[0][0][0]+i*internalX;
//			}
			Log.w("interval:",(interval/14.8)+"");
			String charResult = "";
			String retString="";
			for(int i=0;i<10;i++){
//				internalX = (points[i][6][1]-points[i][0][1])/6;
				for(int j=0;j<10;j++){
//					if(points[i][j][0]!=0||points[i][j][1]!=0){
//						Log.w("2",""+internalY);
//						internalY = (points[6][j][0]-points[0][j][0])/6;

						int x=0;
						int y=0;
//						if(points[i][j][0]<(points[0][j][0]+i*internalY)-4){
//							x=-1;
//						}else if(points[i][j][0]>(points[0][j][0]+i*internalY)+4){
//							x=1;
//						}
//
//						if(points[i][j][1]<(points[i][0][1]+j*internalX)-4){
//							y=-1;
//						}else if(points[i][j][1]>(points[i][0][1]+j*internalX)+4){
//							y=1;
//						}
						float expectX=(points[6][j][0]-points[0][j][0])*i/6+points[0][j][0];
						float expectY=(points[6][j][1]-points[0][j][1])*i/6+points[0][j][1];
						charResult=charResult+"("+formatString(expectX+"",5).substring(0,5)+";"+formatString(expectY+"",5).substring(0,5)+")";
						if(points[i][j][0]==points[19][19][0]&&points[i][j][1]==points[19][19][1]){
							charResult+="*";
							retString+="*";
							continue;
						}

						double shreshhold=3.0;//14.8 is from experience
						if(points[i][j][0]>expectX+3*shreshhold){
							x=1;
						}else if(points[i][j][0]>expectX+shreshhold){
							x=-1;
						}else if(points[i][j][0]<expectX-3*shreshhold){
							x=-2;
						}else if(points[i][j][0]<expectX-shreshhold){
							x=2;
						}

						if(points[i][j][1]<expectY-3*shreshhold){
							y=1;
						}else if(points[i][j][1]<expectY-shreshhold){
							y=-1;
						}else if(points[i][j][1]>expectY+3*shreshhold){
							y=-2;
						}else if(points[i][j][1]>expectY+shreshhold){
							y=2;
						}

						if(x==0){
							if(y==1){
								charResult+="R";
								retString+="R";
							}else if(y==-1){
								charResult+="S";
								retString+="S";
							}else if(y==-2){
								charResult+="T";
								retString+="T";
							}else if(y==2){
								charResult += "U";
								retString+="U";
							}else{
								charResult += "A";
								retString+="A";
							}
						}else if(x==1){
							if(y==1){
								charResult+="B";
								retString+="B";
							}else if(y==-1){
								charResult+="C";
								retString+="C";
							}else if(y==-2){
								charResult+="D";
								retString+="D";
							}else if(y==2){
								charResult += "V";
								retString+="V";
							}else {
								charResult += "E";
								retString+="E";
							}
						}else if(x==-1){
							if(y==1){
								charResult+="F";
								retString+="F";
							}else if(y==-1){
								charResult+="G";
								retString+="G";
							}else if(y==-2){
								charResult+="H";
								retString+="H";
							}else if(y==2){
								charResult += "W";
								retString+="W";
							}else {
								charResult += "I";
								retString+="I";
							}
						}else if(x==-2){
							if(y==1){
								charResult+="J";
								retString+="J";
							}else if(y==-1){
								charResult+="K";
								retString+="K";
							}else if(y==-2){
								charResult+="L";
								retString+="L";
							}else if(y==2){
								charResult += "X";
								retString+="X";
							}else {
								charResult += "M";
								retString+="M";
							}
						}else{
							if(y==1){
								charResult+="N";
								retString+="N";
							}else if(y==-1){
								charResult+="O";
								retString+="O";
							}else if(y==-2){
								charResult+="P";
								retString+="P";
							}else if(y==2){
								charResult += "Y";
								retString+="Y";
							}else {
								charResult += "Q";
								retString+="Q";
							}
						}

//						Log.w("",x+"|"+y);
//					}
				}
//				retString=formatString(retString,i*12+10);
				retString+="\n";
				Log.w("",charResult);charResult="";
			}
//			Log

            return retString;
        }




		//
		return result+"";
	}

	public static void sortPoints(float[][][] points){
		for(int i=0;i<20;i++){
			for(int j=0;j<20;j++){
				if(points[i][j][1]==0f&&points[i][j][0]==0f)
					break;
				for(int k=j+1;k<20;k++){
					if(points[i][k][1]==0f&&points[i][k][0]==0f)
						break;
					if(points[i][j][1]>points[i][k][1])
						swaoPoints(i,j,i,k,points);
				}
			}
		}
	}

	public static void swaoPoints(int i1, int j1,int i2,int j2,float[][][] points){
		float tempX=points[i1][j1][0];
		float tempY=points[i1][j1][1];
		points[i1][j1][0]=points[i2][j2][0];
		points[i1][j1][1]=points[i2][j2][1];
		points[i2][j2][0]=tempX;
		points[i2][j2][1]=tempY;
	}

    private String formatString(String str, int n){
        while(n>str.length()){
            str+=" ";
        }
        return str;
    }

	private class XstartAndEnd{
		int xs=0;
		int xe=0;
		XstartAndEnd(){}

		public void setXs(int xs){
			this.xs=xs;
		}

		public void setXe(int xe){
			this.xe=xe;
		}
	}

	//following is the rotating and finding standard line
	public static ArrayData houghTransform(ArrayData inputData, int thetaAxisSize, int rAxisSize, int minContrast, boolean[][] bitsBool)
	{
		int width = inputData.width;
		int height = inputData.height;
		int maxRadius = (int)Math.ceil(Math.hypot(width, height));
		int halfRAxisSize = rAxisSize >>> 1;
		ArrayData outputData = new ArrayData(thetaAxisSize, rAxisSize);
		// x output ranges from 0 to pi
		// y output ranges from -maxRadius to maxRadius
		double[] sinTable = new double[thetaAxisSize];
		double[] cosTable = new double[thetaAxisSize];
		for (int theta = thetaAxisSize - 1; theta >= 0; theta--)
		{
			double thetaRadians = theta * Math.PI / thetaAxisSize;
			sinTable[theta] = Math.sin(thetaRadians);
			cosTable[theta] = Math.cos(thetaRadians);
		}

		for (int y = height - 1; y >= 0; y--)
		{
			for (int x = width - 1; x >= 0; x--)
			{
//				if (inputData.contrast(x, y, minContrast))
				if(bitsBool[y][x])
				{
					for (int theta = thetaAxisSize - 1; theta >= 0; theta--)
					{
						double r = cosTable[theta] * x + sinTable[theta] * y;
						int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
						outputData.accumulate(theta, rScaled, 1);
					}
				}
			}
		}
		return outputData;
	}

	public static class ArrayData
	{
		public final int[] dataArray;
		public final int width;
		public final int height;

		public ArrayData(int width, int height)
		{
			this(new int[width * height], width, height);
		}

		public ArrayData(int[] dataArray, int width, int height)
		{
			this.dataArray = dataArray;
			this.width = width;
			this.height = height;
		}

		public int get(int x, int y)
		{  return dataArray[y * width + x];  }

		public void set(int x, int y, int value)
		{  dataArray[y * width + x] = value;  }

		public void accumulate(int x, int y, int delta)
		{  set(x, y, get(x, y) + delta);  }

		public boolean contrast(int x, int y, int minContrast)
		{
			int centerValue = get(x, y);
			for (int i = 8; i >= 0; i--)
			{
				if (i == 4)
					continue;
				int newx = x + (i % 3) - 1;
				int newy = y + (i / 3) - 1;
				if ((newx < 0) || (newx >= width) || (newy < 0) || (newy >= height))
					continue;
				if (Math.abs(get(newx, newy) - centerValue) >= minContrast)
					return true;
			}
			return false;
		}

		public int getMax()
		{
			int max = dataArray[0];
			for (int i = width * height - 1; i > 0; i--)
				if (dataArray[i] > max)
					max = dataArray[i];
			return max;
		}
	}

	public static ArrayData getArrayDataFromImage(Bitmap graymap)
	{
		int width = graymap.getWidth();
		int height = graymap.getHeight();
		ArrayData arrayData = new ArrayData(width, height);
		// Flip y axis when reading image
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
//				int rgbValue=graymap.getPixel(x,y);
//				rgbValue = (int)(((rgbValue & 0xFF0000) >>> 16) * 0.30 + ((rgbValue & 0xFF00) >>> 8) * 0.59 + (rgbValue & 0xFF) * 0.11);
				System.out.print((graymap.getPixel(x,y)  & 0xFF )+"|");
				arrayData.set(x, height - 1 - y, (graymap.getPixel(x,y) & 0xFF));
			}
			System.out.println();
		}
		return arrayData;
	}

	private Bitmap binarization(Bitmap bitmap, int lowColor, int highColor) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int pixels[] = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
		Binarizer binarizer = new HybridBinarizer(source);

		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		try {
			BitMatrix matrix = binarizer.getBlackMatrix();
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					if (matrix.get(j, i)) {
						result.setPixel(j, i, highColor);
					} else {
						result.setPixel(j, i, lowColor);
					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static int findLinePoint(Point[] possiblePoints, ArrayList<Point> pointsList){
		if(possiblePoints[0]!=null){
			for(int j=0;j<pointsList.size();j++){
				pointsList.get(j).setDistanceTo(possiblePoints[0]);
			}
			Collections.sort(pointsList);
			System.out.println("nearest1:"+pointsList.get(0).distance);
			if(pointsList.get(0).distance<2) {
				for(int j=0;j<pointsList.size();j++){
					pointsList.get(j).setDistanceTo(possiblePoints[1]);
				}
				Collections.sort(pointsList);
				System.out.println("nearest2:" + pointsList.get(0).distance);
				if(pointsList.get(0).distance<2)
					return 0;
			}
		}
//		for(int i=0;i<2;i++){
//			if(possiblePoints[2*i]!=null){
//				for(int j=0;j<pointsList.size();j++){
//					pointsList.get(j).setDistanceTo(possiblePoints[2*i]);
//				}
//				Collections.sort(pointsList);
//				System.out.println("nearest1:"+pointsList.get(0).distance);
//				if(pointsList.get(0).distance<2) {
//					for(int j=0;j<pointsList.size();j++){
//						pointsList.get(j).setDistanceTo(possiblePoints[2*i+1]);
//					}
//					Collections.sort(pointsList);
//					System.out.println("nearest2:" + pointsList.get(0).distance);
//					if(pointsList.get(0).distance<2)
//						return 2*i;
//				}
//			}
//		}
		return -1;
	}

	public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3,
										 double y3, double x4, double y4) {
        /*
         * A = (x2-x1, y2-y1) B = (x3-x1, y3-y1) C = (x4-x1, y4-y1) D = (x4-x3,
         * y4-y3) = C-B E = (x1-x3, y1-y3) = -B F = (x2-x3, y2-y3) = A-B Result
         * is ((AxB) (AxC) <=0) and ((DxE) (DxF) <= 0) DxE = (C-B)x(-B) =
         * BxB-CxB = BxC DxF = (C-B)x(A-B) = CxA-CxB-BxA+BxB = AxB+BxC-AxC
         */
		x2 -= x1; // A
		y2 -= y1;
		x3 -= x1; // B
		y3 -= y1;
		x4 -= x1; // C
		y4 -= y1;
		double AvB = x2 * y3 - x3 * y2;
		double AvC = x2 * y4 - x4 * y2;
		// Online
		if (AvB == 0.0 && AvC == 0.0) {
			if (x2 != 0.0) {
				return (x4 * x3 <= 0.0)
						|| ((x3 * x2 >= 0.0) && (x2 > 0.0 ? x3 <= x2 || x4 <= x2 : x3 >= x2
						|| x4 >= x2));
			}
			if (y2 != 0.0) {
				return (y4 * y3 <= 0.0)
						|| ((y3 * y2 >= 0.0) && (y2 > 0.0 ? y3 <= y2 || y4 <= y2 : y3 >= y2
						|| y4 >= y2));
			}
			return false;
		}
		double BvC = x3 * y4 - x4 * y3;
		return (AvB * AvC <= 0.0) && (BvC * (AvB + BvC - AvC) <= 0.0);
	}
}
