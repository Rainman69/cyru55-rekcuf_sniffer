package co.bh.rekcuf.sniffer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class NetworkUtil{

	public static boolean isConnected(Context context){
		boolean connected=false;
		try{
			ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo nInfo=cm.getActiveNetworkInfo();
			connected=nInfo!=null&&nInfo.isAvailable()&&nInfo.isConnected();
		}catch(Exception e){
		}
		return connected;
	}

}
