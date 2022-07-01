package co.bh.rekcuf.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;


public class BgSrvRestarter extends BroadcastReceiver{

	@Override
	public void onReceive(Context context,Intent intent){
		SQLite db1=new SQLite(context.getApplicationContext());
		String last_switch_stat=db1.se1("select v from data where k='last_switch_stat';");
		boolean switch_stat=last_switch_stat.equals("0")?false:true;
		if(switch_stat){
			Toast.makeText(context.getApplicationContext(),R.string.run_srvrestarter_toast_restarted,Toast.LENGTH_SHORT).show();
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
				context.startForegroundService(new Intent(context,bgService.class));
			}else{
				context.startService(new Intent(context,bgService.class));
			}
		}
	}

}