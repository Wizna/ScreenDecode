package foster.src.other;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class ProgressView 
{
	private  ProgressDialog MyDialog=null;
	
	public void showProgressD(Activity ct, String strText)
	{
		String strShow =  "connect to network~";
		if(strText != null && strText.length() > 0)
		{
			strShow = strText;
		}
		MyDialog =ProgressDialog.show(ct, " " , strShow, true);
		 Window wd=MyDialog.getWindow();
		 wd.setGravity(Gravity.CENTER);
		 WindowManager.LayoutParams lp=wd.getAttributes();
		 lp.alpha=0.5f;
		 wd.setAttributes(lp);
		 MyDialog.show();
	}
	
	public void closeProgressD()
	{
		this.MyDialog.dismiss();
	}

}
