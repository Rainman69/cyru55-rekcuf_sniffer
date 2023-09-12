package co.bh.rekcuf.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RequiresApi(api=Build.VERSION_CODES.N)
public class bgTile extends TileService{

	public SQLite db1=null;
	public static boolean bgTile_start=false;
	ArrayList<Thread> T=new ArrayList<Thread>();
	NotificationCompat.Builder nb=null;
	NotificationManager manager=null;
	int session_counter=0;
	int session_download=0;

	@Override public void onTileAdded(){
		super.onTileAdded();
		Log.e("__L","@onTileAdded");
		setTileStat(Tile.STATE_INACTIVE);
	}

	@Override public void onStartListening() {
		super.onStartListening();
		Log.e("__L","@onStartListening");

		if(db1==null){
			Log.e("__L","bgTile > onStartListening: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

		Tile tile=getQsTile();
		if(tile!=null){
			setTileStat(Tile.STATE_UNAVAILABLE);
			String last_switch_stat=db1.se1("select v from data where k='last_switch_stat';");
			boolean switch_stat=!last_switch_stat.equals("0");
			int tileStat=0;
			if(switch_stat){
				boolean loc_switch_stat=false;
				boolean act_one=isStaticVarDefined("one","switch_stat");
				try{ loc_switch_stat=one.switch_stat; }catch(Exception ignored){}
				if(act_one){
					tileStat=loc_switch_stat?Tile.STATE_ACTIVE:(bgTile_start?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
				}else{
					if(bgTile_start){
						tileStat=Tile.STATE_ACTIVE;
					}else{
						db1.exe("update data set v='1' where k='tile_killed_bg';");
						db1.exe("update data set v='0' where k='last_switch_stat';");
						Toast.makeText(getApplicationContext(),R.string.tile_killed_bg_toast,Toast.LENGTH_LONG).show();
						if(!T.isEmpty()) srvStop();
						tileStat=Tile.STATE_INACTIVE;
					}
				}
			}else{
				String res1=db1.se1("select count(*) as x from host where valid>0;");
				tileStat=Integer.parseInt(res1)<1?Tile.STATE_UNAVAILABLE:Tile.STATE_INACTIVE;
			}
			setTileStat(tileStat);
		}

	}

	@Override public void onClick(){
		super.onClick();
		Log.e("__L","@onClick");

		boolean loc_switch_stat=false;
		boolean act_one=isStaticVarDefined("one","switch_stat");
		try{ loc_switch_stat=one.switch_stat; }catch(Exception ignored){}

		if(act_one&&loc_switch_stat)
			one.switch_stat=one.switch_by_user=false;

		Tile tile=getQsTile();
		if(tile!=null){
			int tileStat=tile.getState();
			tileSrv(tileStat==Tile.STATE_INACTIVE);
		}

	}

	public void setTileStat(int stat){
		Tile tile=getQsTile();
		if(tile!=null){
			tile.setState(stat);
			tile.updateTile();
		}
	}

	public void tileSrv(boolean turn){
		boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
		if(db1==null){
			Log.e("__L","bgTile > onClick> tileSrv: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}
		if(net_stat){
			db1.exe("update data set v='"+(turn?"1":"0")+"' where k='last_switch_stat';");
			bgTile_start=turn;
			if(turn){
				srvStart();
			}else{
				srvStop();
				if(db1!=null&&!bgTile_start){
					Log.e("__L","Tile Stop, Close DB");
					try{
						db1.close();
						db1=null;
					}catch(Exception ignored){}
				}
			}
		}else{
			Toast.makeText(getApplicationContext(),R.string.run_tile_toast_turnon,Toast.LENGTH_LONG).show();
			if(bgTile_start){
				srvStop();
			}
		}
	}

	public void srvStart(){
		String last_conc=db1.se1("select v from data where k='last_conc';");
		String last_notif=db1.se1("select v from data where k='last_notif';");
		int conc=Integer.parseInt(last_conc);
		int notif=Integer.parseInt(last_notif);
		if(notif>0){
			try{
				if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
					startNotif26();
				}else{
					startNotif();
				}
			}catch(Exception ignored){}
		}
		setTileStat(Tile.STATE_ACTIVE);
		for(int i=0;i<conc;i++){
			Thread t=new Thread(new TileRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		for(Thread t:T){
			t.interrupt();
		}
		stopForeground(true);
		setTileStat(Tile.STATE_INACTIVE);
		if(manager!=null){
			try{
				manager.cancel(12);
			}catch(Exception ignored){}
		}
	}

	public void startNotif(){
		manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nb=new NotificationCompat.Builder(this,"rekcuf.notif2")
			.setOngoing(true)
			.setContentTitle(getString(R.string.tile_title))
			.setContentText("Starting ...")
			.setSmallIcon(R.drawable.ic_tile);
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.N){
			nb.setPriority(NotificationManager.IMPORTANCE_HIGH)
			.setCategory(Notification.CATEGORY_SERVICE);
		}
		startForeground(12,nb.build());
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public void startNotif26(){
		String CHANNEL_ID="rekcuf.notif2";
		NotificationChannel chan=new NotificationChannel(CHANNEL_ID,"BgTileService",NotificationManager.IMPORTANCE_DEFAULT);
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
		startForeground(12,nb.build());
	}

	class TileRunner implements Runnable{

		public void run(){
			while(bgTile_start){
				boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
				if(net_stat){
					HashMap<String,String> addr=SQLite.se1row("select rowid,domain,status from host where valid>0 order by random() limit 1;");
					int rowid=Integer.parseInt(addr.get("rowid"));
					String addr_domain=addr.get("domain");
					int addr_status=Integer.parseInt(addr.get("status"));
					if(addr_domain.length()>0){
						if(addr_domain.length()>3){
							if(addr_status>=300&&addr_status<400)
								addr_domain="www."+addr_domain;
							String url="https://"+addr_domain+"/";
							int stat_int=send_http_request(url);
							++session_counter;
							db1.exe("update data set v=v+1 where k='sent_total';");
							if(rowid>0)
								SQLite.exe("update host set status="+stat_int+", valid=valid"+(stat_int<200?"-":"+")+"1 where rowid="+rowid+";");
							if(bgTile_start && nb!=null && manager!=null){
								int dl_size=session_download/10240;
								float dl_mb=(float)dl_size/100;
								nb.setContentText(getString(R.string.tile_txt_sent)+": "+session_counter+"  "+getString(R.string.tile_txt_dl)+": "+dl_mb+"MB");
								manager.notify(12, nb.build());
							}
							Intent i=new Intent("co.bh.rekcuf.sniffer");
							i.putExtra("stat",Integer.toString(stat_int));
							i.putExtra("domain",addr_domain);
							sendBroadcast(i);
						}
					}
				}else{
					bgTile_start=false;
					srvStop();
				}
			}
		}

	}

	public int send_http_request(String str){
		int responseCode=-1;
		int http_status=0;
		//String content="";
		String last_timeout=db1.se1("select v from data where k='last_timeout';");
		int timeout=Integer.parseInt(last_timeout);
		timeout=Math.floorDiv(timeout,2);
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

			if(responseCode==200){
				int cl=urlConn.getContentLength();
				if(cl<0 && Build.VERSION.SDK_INT>VERSION_CODES.M)
					cl=(int)urlConn.getHeaderFieldLong("Content-Length",-1);
				if(cl>0)
					session_download+=cl;
			}

			/*if(responseCode==HttpURLConnection.HTTP_OK){
				String q;
				do{
					q=br.readLine();
					content+=q;
				}while(q!=null);
				br.close();
			}*/
			urlConn.disconnect();
			//Toast.makeText(getApplicationContext(),"content len: "+len,Toast.LENGTH_SHORT).show();
		}catch(Exception e){
			e.printStackTrace();
		}
		Log.println(Log.ERROR,"__L","responseCode--------> "+responseCode);
		Log.println(Log.ERROR,"__L","http_status--------> "+http_status);
		return responseCode;
	}

	public boolean isStaticVarDefined(String className,String varName){
		try{
			Class<?> clazz=Class.forName("co.bh.rekcuf.sniffer."+className);
			Field field=clazz.getDeclaredField(varName);
			return Modifier.isStatic(field.getModifiers());
		}catch(Exception e){
			return false;
		}
	}

}
