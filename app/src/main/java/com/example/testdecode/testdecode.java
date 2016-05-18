package com.example.testdecode;

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.lang.Thread;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import com.google.zxing.Android.PlanarYUVLuminanceSource;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.PerspectiveTransform;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;

import android.graphics.Bitmap;

import android.graphics.Matrix;
import android.hardware.Camera;

import android.media.AudioManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.content.SharedPreferences;


import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;




public class testdecode extends Activity implements Runnable
{
	/** Called when the activity is first created. */
//	boolean whetherCatch=false;
	private final String PREFERENCES_NAME = "decodesetting";
	
	private SurfaceView sfvCamera;
	private SFHCamera sfhCamera;
	private ImageView imgView;
//	private Button btn01;
	
	int width;
	int height;
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

	AudioManager audioManager;
	
	byte[] myYUV;
	long retNumber=0;
	Bitmap mBitmap;
	PlanarYUVLuminanceSource source;
	byte[] tempData;
//	boolean bClickBtn = false;
	Thread t1;
	private DecodeThread myDecodThread;

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        setContentView(R.layout.testdecode);

        audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);

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
//		btn01=(Button) this.findViewById(R.id.btn01);
//		btn01.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				whetherCatch=!whetherCatch;
//			}
//		});
		sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight,previewCallback, errorCallback);
		sfhCamera.parentActivity=this;
		
		SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		m_nImgProcessTh= preferences.getInt("ImgProcessTh",6);
		m_nPixDownDosTh = preferences.getInt("DotsDownTh",4);
	    m_bCMYKMode = preferences.getBoolean("BCMYKMode",false);
		m_bInvertColorMode = preferences.getBoolean("BInvertColor", false);
		flag_DecodeMode = preferences.getInt("CodeType", 0);

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
		Log.i("*", "startGetImgFrame");
		System.out.println("start frame time:"+System.currentTimeMillis());
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
				sfhCamera=null;
				sfhCamera = new SFHCamera(sfvCamera.getHolder(), screenWidth, screenHeight,previewCallback, errorCallback);
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
				    		if(retNumber==0 || retNumber==-1)
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
 
//    @Override
//    public void onClick(View view)
//    {
//    	bClickBtn = true;
//    	bClickBtn = false;
//    }

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
							Log.i("*", "cancel is clicked");
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

			retNumber=0;
			String retString="";

			//now add my code to replace original decode process using .so
			try {
				retString = decodeBitMap(bitmap,mBitmap);

			} catch (NotFoundException e) {
				e.printStackTrace();
			}

			Log.i("result", retString);

			if(retString.length()>5&&retString.contains("\n")) {
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

				Intent intent=new Intent(testdecode.this,ScanResult.class);
				intent.putExtra("flag", 2);
				intent.putExtra("type", flag_DecodeMode);
				Date now=new Date(System.currentTimeMillis());
				intent.putExtra("time", now.toString());
				intent.putExtra("bmp", bytes);
				intent.putExtra("number", retString);
				startActivity(intent);

			}
		}
	}
	
	public void onResume()
    {
		Log.i("*", "onResume");
        super.onResume();
    }

	public String decodeBitMap(BinaryBitmap bitmap,Bitmap grayMap) throws NotFoundException {
		long[] times=new long[10];
		times[0]=System.currentTimeMillis();

        boolean[][] whetherChecked=new boolean[grayMap.getHeight()][grayMap.getWidth()];
        float[][][] points=new float[20][20][2];
        int indexI=0;
        int indexJ=0;
        boolean alreadyOne=false;
        boolean lineChange=false;

		//now add find direction and chose edge line
		ArrayList<Point> pointArrayList=new ArrayList<>();

		//try zxing binarization
		BitMatrix binaryMatrix=bitmap.getBlackMatrix();

		//get bitmap from bitmatrix
		int bitMapWidth=binaryMatrix.getWidth();
		int bitMapHeight=binaryMatrix.getHeight();

		final int WHITE = 0xFFFFFFFF;
		final int BLACK = 0xFF000000;

		int[] pixels = new int[bitMapWidth * bitMapHeight];
		for (int y = 0; y < bitMapHeight; y++) {
			int offset = y * bitMapWidth;
			for (int x = 0; x < bitMapWidth; x++) {
				pixels[offset + x] = binaryMatrix.get(x, y) ? WHITE : BLACK;
			}
		}
		grayMap.setPixels(pixels,0,binaryMatrix.getWidth(),0,0,binaryMatrix.getWidth(),binaryMatrix.getHeight());

		times[1]=System.currentTimeMillis();
		Log.w("before rotate ",""+bitMapWidth+"|"+bitMapHeight);


		int[][] changePixelCenter  = new int[4][2];
		initialTourPixel(changePixelCenter);
		//find theta, with finding nearest points and extend, first find points' x and y
		for (int j = 0; j < bitMapWidth; j++) {
			for (int i = 0; i < bitMapHeight; i++) {
				if(!whetherChecked[i][j]&& (0xF&pixels[(bitMapHeight-1-i) * bitMapWidth+j])!=0){

					int originX=j;
					int originY=i;

					int changeX=j;
					int changeY=i;

					int lastMove=1;

					int xs=j,xe=j,ys=i,ye=i;

					while(true){
						for(int k=0;k<4;k++){
							int tempMoveDirection=(lastMove+k+3)%4;
							int tempX=changeX+changePixelCenter[tempMoveDirection][0];
							int tempY=changeY+changePixelCenter[tempMoveDirection][1];

							if(tempX>-1&&tempY>-1&&tempX<bitMapWidth&&tempY<bitMapHeight&&!whetherChecked[tempY][tempX]&&(0xF&pixels[(bitMapHeight-1-tempY) * bitMapWidth+tempX])!=0){
								changeX=tempX;
								changeY=tempY;
								lastMove=tempMoveDirection;
								break;
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

					for(int setI=ys;setI<=ye;setI++){
						for(int setJ=xs;setJ<=xe;setJ++){
							whetherChecked[setI][setJ] = true;
						}
					}

					float centX = ((float)xs+xe)/2;
					float centY = ((float)ys+ye)/2;

					pointArrayList.add(new Point(centX,centY));
				}
			}
		}

		if(pointArrayList.size()<49){
			return "not enough points";
		}
		Log.w("e:","1");
		times[2]=System.currentTimeMillis();
		//finding theta
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
			for (int i = 1; i < 5; i++) {
				fourPoints[i - 1][0] = pointArrayList.get(i).getCentX();
				fourPoints[i - 1][1] = pointArrayList.get(i).getCentY();
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
//				Log.w("rate:",""+ratePair+"|"+Math.hypot((fourPoints[m][1] - fourPoints[n][1]), (fourPoints[m][0] - fourPoints[n][0])) / disPair);
				if (1.05263 > ratePair && ratePair > 0.95 && Math.hypot((fourPoints[m][1] - fourPoints[n][1]), (fourPoints[m][0] - fourPoints[n][0])) / disPair > 0.95) {
					possiblePoints[0] = new Point((2 * pointArrayList.get(m+1).centX - midPoint.centX), (2 * pointArrayList.get(m+1).centY - midPoint.centY));
					possiblePoints[1] = new Point((2 * pointArrayList.get(n+1).centX - midPoint.centX), (2 * pointArrayList.get(n+1).centY - midPoint.centY));

					int findResult = findLinePoint(possiblePoints, pointArrayList);
					if (findResult != -1){
						whetherFound = true;

						foundTheta=Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180;
						System.out.println(Math.atan((fourPoints[m][1] - fourPoints[n][1]) / (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180);
						System.out.print(midPoint.centX+"|"+midPoint.centY+"\n"+
								possiblePoints[0].centX+"|"+possiblePoints[0].centY+"\n"+
								possiblePoints[1].centX+"|"+possiblePoints[1].centY+"\n"+
								pointArrayList.get(m+1).centX+"|"+pointArrayList.get(m+1).centY+"\n"+
								pointArrayList.get(n+1).centX+"|"+pointArrayList.get(n+1).centY+"\n");
						break;
					}
				}
			}
		}

		Log.w("e:","2");
		if(!whetherFound)
			return "-1"+countSelectingPoint;

		times[3]=System.currentTimeMillis();
		//rotate
		Matrix tempTransMatrix = new Matrix();
		tempTransMatrix.postRotate((float)(foundTheta-90));
		grayMap= Bitmap.createBitmap(grayMap, 0, 0, grayMap.getWidth(), grayMap.getHeight(), tempTransMatrix, true);
		//end of rotate

		times[4]=System.currentTimeMillis();

		whetherChecked=new boolean[grayMap.getHeight()][grayMap.getWidth()];
		bitMapWidth=grayMap.getWidth();
		bitMapHeight=grayMap.getHeight();
		Log.w("after rotate ",""+grayMap.getWidth()+"|"+grayMap.getHeight());

		//now start finding points after rotate
		//This one can be deleted and use getpixels of bitmap
		pixels=new int[grayMap.getWidth()*grayMap.getHeight()];
		grayMap.getPixels(pixels,0,bitMapWidth,0,0,bitMapWidth,bitMapHeight);
		times[5]=System.currentTimeMillis();

		boolean firstRound=true;
		double interval=0.0;
		for (int j = 0; j < bitMapWidth; j++) {
			for (int i = 0; i < bitMapHeight; i++) {
				if(whetherChecked[i][j]){
					lineChange=true;
					alreadyOne=true;
				}else
				if((0xF&pixels[(bitMapHeight-1-i) * bitMapWidth+j])!=0){

					int originX=j;
					int originY=i;

					int changeX=j;
					int changeY=i;

					int lastMove=1;

					int xs=j,xe=j,ys=i,ye=i;

					while(true){
						for(int k=0;k<4;k++){
							int tempMoveDirection=(lastMove+k+3)%4;
							int tempX=changeX+changePixelCenter[tempMoveDirection][0];
							int tempY=changeY+changePixelCenter[tempMoveDirection][1];

							if(tempX>-1&&tempY>-1&&tempX<bitMapWidth&&tempY<bitMapHeight&&!whetherChecked[tempY][tempX]&&(0xF&pixels[(bitMapHeight-1-tempY) * bitMapWidth+tempX])!=0){
								changeX=tempX;
								changeY=tempY;
								lastMove=tempMoveDirection;
								break;
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


					for(int setI=ys;setI<=ye;setI++){
						for(int setJ=xs;setJ<=xe;setJ++){
							whetherChecked[setI][setJ] = true;
						}
					}

					float centX = ((float)xs+xe)/2;
					float centY = ((float)ys+ye)/2;

					//remove spots
					if(Math.abs(xe-xs)>=2||Math.abs(ye-ys)>=2)
						pointArrayList.add(new Point(centX,centY));

					//the second half is to make segment line into one
					if(firstRound||(centX-points[0][0][0]<0.5*interval)) {
						points[indexI][indexJ][0] = centX;
						points[indexI][indexJ][1] = centY;
					}else
						for(int m=0;m<(20-1);m++){
							if(points[0][m+1][1]==0f&&(Math.abs(centY-points[0][m][1])>0.5*interval)) {
								Log.w("already", " exceed limit !!!!");
								break;
							}
							if(Math.abs(centY-points[0][m][1])<Math.abs(centY-points[0][m+1][1])&&(Math.abs(centY-points[0][m][1])<0.5*interval)){

								double tempIndexI=(centX-points[0][m][0])/interval;
								double floorIndexI=Math.floor(tempIndexI);
								if(tempIndexI-floorIndexI>0.5)
									indexI=(int)floorIndexI+1;
								else
									indexI=(int)floorIndexI;

								if((points[indexI][m][0]==0f&&points[indexI][m][1]==0f)||(m==0&&centY>points[indexI][m][1])){
									points[indexI][m][0] = centX;
									points[indexI][m][1] = centY;
								}else if(m!=0){
									Log.w("not proper points:",""+centX+"|"+centY);
								}

								break;
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

				if(indexI<19&&!firstRound) {

					indexI++;
					indexJ=0;
				}

				alreadyOne=false;
			}

			lineChange=false;

		}

		times[6]=System.currentTimeMillis();
		//add perspective transform, 400 is the possible points in the picture, can vary for different phones
		float[] xValues=new float[400];
		float[] yValues=new float[400];

		int foundStartPoint=0;
		for(int i=0;i<20-6;i++){
			if((points[6][i][0]!=0.0||points[6][i][1]!=0.0)&&(points[6][i+6][0]!=0.0||points[6][i+6][1]!=0.0))
				break;
			else
				foundStartPoint++;
		}

		if(foundStartPoint==20-6){
			return "-1";
		}

		for(int i=0;i<5;i++){
			double disFtoS=Math.hypot((points[6][foundStartPoint+i][0]-points[6][foundStartPoint+i+1][0]),(points[6][foundStartPoint+i][1]-points[6][foundStartPoint+i+1][1]));
			double disStoT=Math.hypot((points[6][foundStartPoint+i+1][0]-points[6][foundStartPoint+i+2][0]),(points[6][foundStartPoint+i+1][1]-points[6][foundStartPoint+i+2][1]));
			double disFtoT=Math.hypot((points[6][foundStartPoint+i][0]-points[6][foundStartPoint+i+2][0]),(points[6][foundStartPoint+i][1]-points[6][foundStartPoint+i+2][1]));
			double tempRate=disFtoS/disStoT;
			if(1.05263<tempRate||tempRate<0.95||(disFtoT/(disFtoS+disStoT))<0.95)
				return "second standard line failed to be straight segments";

		}

		PerspectiveTransform perspectiveTransform=PerspectiveTransform.quadrilateralToQuadrilateral(
				points[0][foundStartPoint][0], points[0][foundStartPoint][1],
				points[0][foundStartPoint+6][0], points[0][foundStartPoint+6][1],
				points[6][foundStartPoint][0], points[6][foundStartPoint][1],
				points[6][foundStartPoint+6][0], points[6][foundStartPoint+6][1],
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

		String charResult = "";
		String retString="";
		for(int i=0;i<10;i++){
			for(int j=0;j<10;j++){
					int x=0;
					int y=0;
					float expectX=(points[6][j][0]-points[0][j][0])*i/6+points[0][j][0];
					float expectY=(points[6][j][1]-points[0][j][1])*i/6+points[0][j][1];
					charResult=charResult+"("+formatString(expectX+"",5).substring(0,5)+";"+formatString(expectY+"",5).substring(0,5)+")";
					if((points[i][j][0]==points[19][19][0]&&points[i][j][1]==points[19][19][1])||(points[6][j][0]==points[19][19][0]&&points[6][j][1]==points[19][19][1])){
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

			}
			retString+="\n";
			Log.w("p:",charResult);charResult="";
		}

		times[7]=System.currentTimeMillis();
		for(int i=0;i<10;i++){
			System.out.println("time:"+times[i]);
		}

		int countT = retString.length() - retString.replace("T", "").length();
		int countR = retString.length() - retString.replace("R", "").length();
		if( countT > countR ){
			retString=retString.replace("H", "N").replace("T", "R").replace("P", "F").replace("L", "B")
					.replace("X", "C").replace("K", "V").replace("M", "E")
					.replace("W", "1").replace("U", "2").replace("Y", "3").replace("I", "4")
					.replace("Q", "I").replace("G", "Y").replace("S", "U").replace("O", "W")
					.replace("1", "O").replace("2", "S").replace("3", "G").replace("4", "Q");

			return new StringBuilder(retString).reverse().toString().substring(1);
		}


		return retString;
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
						swapPoints(i, j, i, k, points);
				}
			}
		}
	}

	public static void swapPoints(int i1, int j1, int i2, int j2, float[][][] points){
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
        return str.substring(0,n);
    }

	public static int findLinePoint(Point[] possiblePoints, ArrayList<Point> pointsList){
		if(possiblePoints[0]!=null){
			for(int j=0;j<pointsList.size();j++){
				pointsList.get(j).setDistanceTo(possiblePoints[0]);
			}
			Collections.sort(pointsList);
//			System.out.println("nearest1:"+pointsList.get(0).distance);
			if(pointsList.get(0).distance<2) {
				for(int j=0;j<pointsList.size();j++){
					pointsList.get(j).setDistanceTo(possiblePoints[1]);
				}
				Collections.sort(pointsList);
//				System.out.println("nearest2:" + pointsList.get(0).distance);
				if(pointsList.get(0).distance<2)
					return 0;
			}
		}
		return -1;
	}

	static void initialTourPixel(int[][] changePixelCenter){
		changePixelCenter[0][0]=1;
		changePixelCenter[0][1]=0;
		changePixelCenter[1][0]=0;
		changePixelCenter[1][1]=1;
		changePixelCenter[2][0]=-1;
		changePixelCenter[2][1]=0;
		changePixelCenter[3][0]=0;
		changePixelCenter[3][1]=-1;
	}
}
