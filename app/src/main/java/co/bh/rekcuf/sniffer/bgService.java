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
import android.widget.EditText;

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
				startNotif();
			}catch(Exception ignored){}
		}
		for(int i=0;i<conc;i++){
			Thread t=new Thread(new ServiceRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		if(wl1!=null/*&&wl1.isHeld()*/)
			try{wl1.release();}catch(Exception ignored){}
		for(Thread t:T) t.interrupt();
		if(manager!=null){
			try{
				manager.cancel(11);
				manager.cancelAll();
				manager=null;
			}catch(Exception ignored){}
		}
		stopForeground(true);
	}

	public void startNotif(){
		manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
			NotificationChannel chan=new NotificationChannel("rekcuf.notif2","BgTileService",NotificationManager.IMPORTANCE_DEFAULT);
			chan.enableLights(false);
			chan.setSound(null, null);
			manager.createNotificationChannel(chan);
		}
		nb=new NotificationCompat.Builder(this,"rekcuf.notif2")
			.setOngoing(true)
			.setContentTitle(getString(R.string.tile_title))
			.setContentText("Starting ...")
			.setSmallIcon(R.drawable.ic_tile)
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_tile));
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.N){
			nb.setPriority(NotificationManager.IMPORTANCE_HIGH)
			.setCategory(Notification.CATEGORY_SERVICE);
		}
		startForeground(11,nb.build());
	}

	class ServiceRunner implements Runnable{

		public void run(){
			while(mem("switch").equals("1")){
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
							int sent_total_int=0;
							String sent_total=mem("sent_total");
							if(sent_total.equals("")) sent_total=SQLite.se1("select v from data where k='sent_total';");
							if(sent_total.length()>0) sent_total_int=Integer.parseInt(sent_total);
							++sent_total_int;
							mem("sent_total",String.valueOf(sent_total_int));
							SQLite.exe("update data set v="+sent_total_int+" where k='sent_total';");
							if(rowid>0)
								SQLite.exe("update host set status="+stat_int+", valid=valid"+(stat_int<200?"-":"+")+"1 where rowid="+rowid+";");
							if(mem("switch").equals("1") && nb!=null && manager!=null){
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
					mem("switch","0");
					srvStop();
				}
			}
		}

	}

	public String mem(String key){
		String sw="";
		try{sw=SQLite.mem.get(key);}catch(Exception ignored){}
		return sw!=null&&sw.length()>0?sw:"";
	}
	public String mem(String key,String val){
		if(key.length()>0){
			Log.e("__E","bg ServiceRunner > SET mem["+key+"]="+val);
			try{SQLite.mem.put(key,val);}catch(Exception ignored){}
		}return val;
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
			//Log.println(Log.ERROR,"","--------> "+responseCode);
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
			//Log.println(Log.ERROR,"","=====> "+http_status);

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
