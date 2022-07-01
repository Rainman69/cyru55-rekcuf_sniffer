package co.bh.rekcuf.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class bgService extends Service{

	ArrayList<Thread> T=new ArrayList<Thread>();

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public int onStartCommand(Intent intent,int flags,int startId){
		srvStart();
		return super.onStartCommand(intent,flags,startId);
	}

	@Override
	public void onCreate(){
		SQLite db1=new SQLite(bgService.this);// init and create tables
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Intent broadcastIntent=new Intent();
		broadcastIntent.setAction("restartservice");
		broadcastIntent.setClass(this,BgSrvRestarter.class);
		this.sendBroadcast(broadcastIntent);
		//db1.close();
		srvStop();
	}

	public void srvStart(){
		int conc=one.conc;
		if(one.notif){
			if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
				startNotif();
			}else{
				startForeground(1,new Notification());
			}
		}
		for(int i=0;i<conc;i++){
			Thread t=new Thread(new ServiceRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		for(Thread t: T){
			t.interrupt();
		}
		stopForeground(true);
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public void startNotif(){
		String NOTIFICATION_CHANNEL_ID="example.permanence";
		NotificationChannel chan=new NotificationChannel(NOTIFICATION_CHANNEL_ID,"Background Service",NotificationManager.IMPORTANCE_NONE);
		chan.setLightColor(R.color.notif_blue);
		NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager!=null;
		manager.createNotificationChannel(chan);
		NotificationCompat.Builder notificationBuilder=new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
		Notification notification=notificationBuilder.setOngoing(true).setContentTitle("App is running in background").setPriority(NotificationManager.IMPORTANCE_HIGH).setCategory(Notification.CATEGORY_SERVICE).build();
		startForeground(2,notification);
	}

	class ServiceRunner implements Runnable{

		public void run(){
			while(one.switch_stat){
				String domain=SQLite.se1("select domain from host order by random() limit 1;");
				if(domain.length()>0){
					if(domain.length()>3){
						String url="https://"+domain+"/";
						int stat_int=send_http_request(url);
						SQLite.exe("update data set v=v+1 where k='sent_total';");
						Intent i=new Intent("co.bh.rekcuf.sniffer");
						i.putExtra("stat",Integer.toString(stat_int));
						i.putExtra("domain",domain);
						sendBroadcast(i);
					}
				}
			}
		}

	}

	public int send_http_request(String str){
		int responseCode=-1;
		int timeout=one.timeout;
		try{
			URL url=new URL(str);
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			urlConn.setConnectTimeout(timeout);
			urlConn.setReadTimeout(timeout);
			BufferedReader br=new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			urlConn.connect();
			responseCode=urlConn.getResponseCode();
			urlConn.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		return responseCode;
	}

}
