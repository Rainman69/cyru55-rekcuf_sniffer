package co.bh.rekcuf.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;


public class BgSrvRestarter extends BroadcastReceiver{
	@Override
	public void onReceive(Context context,Intent intent){
		if(one.switch_stat){
			Toast.makeText(context,"rekcuF Sniffer is ON",Toast.LENGTH_SHORT).show();
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
				context.startForegroundService(new Intent(context,bgService.class));
			}else{
				context.startService(new Intent(context,bgService.class));
			}
		}
	}
}