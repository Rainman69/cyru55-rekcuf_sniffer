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
import java.util.Date;


public class bgService extends Service{

	public static final String pn="co.bh.rekcuf.sniffer";

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public int onStartCommand(Intent intent,int flags,int startId){
		//Toast.makeText(getApplicationContext(),"onStartCommand",Toast.LENGTH_SHORT).show();
		return super.onStartCommand(intent,flags,startId);
	}

	@Override
	public void onCreate(){
		int conc=one.conc;
		if(one.notif){
			if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
				startNotif();
			}else{
				startForeground(1,new Notification());
			}
		}
		for(int i=0;i<conc;i++){
			new Thread(new Runner(),"Runner"+conc).start();
		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Intent broadcastIntent=new Intent();
		broadcastIntent.setAction("restartservice");
		broadcastIntent.setClass(this,BgSrvRestarter.class);
		this.sendBroadcast(broadcastIntent);
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

	class Runner implements Runnable{
		public void run(){
			while(one.switch_stat){
				String domain=SQLite.se1("select domain from host order by random() limit 1;");
				if(domain.length()>0){
					if(domain.length()>3){
						String url="https://"+domain+"/?";
						int stat_int=send_http_request(url);
						int ts=(int)(new Date().getTime()/1000);
						SQLite.ins("log",new String[]{
							"ts",Integer.toString(ts),
							"stat",Integer.toString(stat_int),
							"domain",domain
						});
						//SQLite.exe("insert into log(ts,stat,domain) values(strftime('%s', 'now'),"+stat_int+",'"+domain+"');");
						SQLite.exe("insert into data(k,v) values('sent_total',1) on conflict(k) do update set v=v+1;");
						Intent i=new Intent("co.bh.rekcuf.sniffer");
						i.putExtra("haveExtra",1);
						sendBroadcast(i);

					}
				}
			}
		}
	}

	public int send_http_request(String str){
		int responseCode=-1;
		//String content="";
		int timeout=one.timeout;
		try{
			URL url=new URL(str);
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			urlConn.setConnectTimeout(timeout);
			urlConn.setReadTimeout(timeout);
			BufferedReader br=new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			//urlConn.setAllowUserInteraction(false);
			//urlConn.setDoInput(true);
			urlConn.connect();
			responseCode=urlConn.getResponseCode();
			/*if(responseCode==HttpURLConnection.HTTP_OK){
				String q;
				do{
					q=br.readLine();
					content+=q;
				}while(q!=null);
				br.close();
			}*/
			urlConn.disconnect();
			/*int len=content.length();
			if(len>0){
				Toast.makeText(getApplicationContext(),"content len: "+len,Toast.LENGTH_SHORT).show();
			}*/
		}catch(Exception e){
			e.printStackTrace();
		}
		return responseCode;
	}

}
