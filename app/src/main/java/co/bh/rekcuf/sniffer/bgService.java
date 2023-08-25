package co.bh.rekcuf.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class bgService extends Service{

	public SQLite db1=null;
	public PowerManager.WakeLock wl1=null;
	ArrayList<Thread> T=new ArrayList<Thread>();
	NotificationCompat.Builder nb=null;
	NotificationManager manager=null;
	int session_counter=0;
	int session_download=0;

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
		super.onCreate();
		if(db1==null){
			Log.e("__L","bgService > onCreate: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}
		try{
			PowerManager pm1=(PowerManager)getSystemService(Context.POWER_SERVICE);
			wl1=pm1.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,"pm:wake1");
		}catch(Exception e){e.printStackTrace();}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Intent broadcastIntent=new Intent();
		broadcastIntent.setAction("restartservice");
		broadcastIntent.setClass(this,BgSrvRestarter.class);
		this.sendBroadcast(broadcastIntent);
		srvStop();
	}

	public void srvStart(){
		if(wl1!=null) wl1.acquire();
		int conc=one.conc;
		if(one.notif){
			try{
				if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
					startNotif26();
				}else{
					startNotif();
					//startForeground(12,new Notification());
				}
			}catch(Exception e){}
		}
		for(int i=0;i<conc;i++){
			Thread t=new Thread(new ServiceRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		if(wl1!=null&&wl1.isHeld()) wl1.release();
		for(Thread t: T){
			t.interrupt();
		}
		stopForeground(true);
		if(manager!=null){
			try{
				manager.cancel(11);
			}catch(Exception e){}
		}
	}

	public void startNotif(){
		manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nb=new NotificationCompat.Builder(this,"rekcuf.notif1")
			.setOngoing(true)
			.setContentTitle(getString(R.string.tile_title))
			.setContentText("Starting ...")
			.setSmallIcon(R.drawable.ic_tile);
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.N){
			nb.setPriority(NotificationManager.IMPORTANCE_HIGH)
			.setCategory(Notification.CATEGORY_SERVICE);
		}
		startForeground(11,nb.build());
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public void startNotif26(){
		String CHANNEL_ID="rekcuf.notif1";
		NotificationChannel chan=new NotificationChannel(CHANNEL_ID,"BgTileService",NotificationManager.IMPORTANCE_NONE);
		chan.enableLights(false);
		chan.setSound(null, null);
		manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager!=null;
		manager.createNotificationChannel(chan);
		nb=new NotificationCompat.Builder(this,CHANNEL_ID)
			.setOngoing(true)
			.setContentTitle(getString(R.string.tile_title))
			.setContentText("Starting ...")
			.setSmallIcon(R.drawable.ic_tile)
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_tile))
			.setPriority(NotificationManager.IMPORTANCE_HIGH)
			.setCategory(Notification.CATEGORY_SERVICE);
		startForeground(11,nb.build());
	}

	class ServiceRunner implements Runnable{

		public void run(){
			while(one.switch_stat){
				if(one.net_stat){
					HashMap<String,String> addr=SQLite.se1row("select rowid,domain,valid,status from host where valid>0 order by random() limit 1;");
					int rowid=Integer.parseInt(addr.get("rowid"));
					String addr_domain=addr.get("domain");
					int addr_valid=Integer.parseInt(addr.get("valid"));
					int addr_status=Integer.parseInt(addr.get("status"));
					if(addr_domain.length()>0){
						if(addr_domain.length()>3){
							if(addr_status>=300&&addr_status<400)
								addr_domain="www."+addr_domain;
							String url="https://"+addr_domain+"/";
							int stat_int=send_http_request(url);
							++session_counter;
							SQLite.exe("update data set v=v+1 where k='sent_total';");
							if(rowid>0)
								SQLite.exe("update host set status="+stat_int+", valid=valid"+(stat_int<200?"-":"+")+"1 where rowid="+rowid+";");
							if(one.switch_stat && nb!=null && manager!=null){
								int dl_size=session_download/10240;
								float dl_mb=(float)dl_size/100;
								nb.setContentText(getString(R.string.tile_txt_sent)+": "+session_counter+"  "+getString(R.string.tile_txt_dl)+": "+dl_mb+"MB");
								manager.notify(11, nb.build());
							}
							Intent i=new Intent("co.bh.rekcuf.sniffer");
							i.putExtra("stat",(addr_valid==1&&stat_int<200?"-2":Integer.toString(stat_int)));
							i.putExtra("domain",addr_domain);
							sendBroadcast(i);
						}
					}
				}else{
					one.switch_stat=false;
					srvStop();
				}
			}
		}

	}

	public int send_http_request(String str){
		int responseCode=-1;
		int timeout=Math.floorDiv(one.timeout,2);
		try{
			URL url=new URL(str);
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			urlConn.setFollowRedirects(false);
			urlConn.setConnectTimeout(timeout);
			urlConn.setReadTimeout(timeout);
			urlConn.setUseCaches(false);
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O){
				urlConn.setRequestProperty("Accept-Encoding","identity");
			}
			//BufferedReader br=new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			urlConn.connect();
			responseCode=urlConn.getResponseCode();
			Log.println(Log.ERROR,"","--------> "+responseCode);
			int http_status=0;
			String headerValue="";
			for(String headerKey: urlConn.getHeaderFields().keySet()){
				headerValue=urlConn.getHeaderField(headerKey);
				break;
			}
			Pattern p=Pattern.compile("HTTP/.+\\s(\\d+)");
			Matcher m=p.matcher(headerValue);
			if(m.find()){
				http_status=Integer.parseInt(m.group(1));
			}
			Log.println(Log.ERROR,"","=====> "+http_status);

			if(responseCode==200){
				int cl=urlConn.getContentLength();
				if(cl<0 && Build.VERSION.SDK_INT>VERSION_CODES.M)
					cl=(int)urlConn.getHeaderFieldLong("Content-Length",-1);
				if(cl>0)
					session_download+=cl;
			}
			urlConn.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		return responseCode;
	}

}
