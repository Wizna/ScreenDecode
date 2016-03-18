package com.example.testdecode;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity 
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        setContentView(R.layout.activity_main);
        Start();
    }
 
    public void Start()
    {
                new Thread()
                {
                        public void run() 
                        {
                                try
                                {
                                   Thread.sleep(2500);
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                                setParament();
                                Intent intent = new Intent(MainActivity.this,testdecode.class);
                                startActivity(intent);
                                finish();
                        }
                }.start();
        }
    

    protected void onRestart() 
    { 
    	super.onRestart(); 
        MyApplication mApp = (MyApplication)getApplication(); 
        if(mApp.isExit()) 
        { 
           finish();
        }
    } 
    
    private void setParament()
    {
    	MyApplication mApp = (MyApplication)getApplication(); 
    	
		 FeatureInfo[] feature=this.getPackageManager().getSystemAvailableFeatures();
		 for (FeatureInfo featureInfo : feature)
		 {
		    if (PackageManager.FEATURE_CAMERA_FLASH.equals(featureInfo.name))
		    {
		         mApp.setFlashlightBool(true); 
		         break;
		    }
		 }
    }
}




