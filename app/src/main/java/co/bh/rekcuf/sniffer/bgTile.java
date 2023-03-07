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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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

	@Override
	public void onTileAdded(){
		setTileStat(false);
		super.onTileAdded();
	}

	@Override
	public void onStartListening() {
		super.onStartListening();

		if(db1==null){
			Log.e("__L","bgTile > onStartListening: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

		String res1=db1.se1("select count(*) as x from host;");
		int db_count=Integer.parseInt(res1);
		if(db_count<1){
			Tile tile=getQsTile();
			if(tile!=null){
				tile.setState(Tile.STATE_UNAVAILABLE);
				tile.updateTile();
			}
		}

	}

	@Override
	public void onClick(){
		super.onClick();

		Tile tile=getQsTile();
		int tileStat=tile.getState();
		if(tile!=null){
			tileSrv(tileStat==Tile.STATE_INACTIVE);
		}

		if(db1==null){
			Log.e("__L","bgTile > onClick: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

		if(tileStat==Tile.STATE_ACTIVE){
			if(db1!=null){
				Log.e("__L","bgTile > onClick: Try Close DB");
				try{
					db1.close();
					db1=null;
				}catch(Exception e){e.printStackTrace();}
			}
		}

	}

	public void setTileStat(boolean stat){
		Tile tile=getQsTile();
		if(tile!=null){
			tile.setState(stat?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
			tile.updateTile();
		}
	}

	public void tileSrv(boolean turn){
		boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
		if(net_stat){
			//try{one.switch_stat=turn;}catch(Exception e){}
			//db1.exe("update data set v='"+(turn?"1":"0")+"' where k='last_switch_stat';");
			bgTile_start=turn;
			setTileStat(turn);
			if(turn){
				srvStart();
			}else{
				srvStop();
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
					//startForeground(12,new Notification());
				}
			}catch(Exception e){}
		}
		for(int i=0;i<conc;i++){
			Thread t=new Thread(new TileRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		for(Thread t: T){
			t.interrupt();
		}
		stopForeground(true);
		if(manager!=null){
			try{
				manager.cancel(12);
			}catch(Exception e){}
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
				//String last_net_stat=db1.se1("select v from data where k='last_net_stat';");
				//if(!last_net_stat.equals("0")){
				boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
				if(net_stat){
					String domain=db1.se1("select domain from host order by random() limit 1;");
					if(domain.length()>0){
						if(domain.length()>3){
							String url="https://"+domain+"/";
							int stat_int=send_http_request(url);
							++session_counter;
							db1.exe("update data set v=v+1 where k='sent_total';");
							if(bgTile_start && nb!=null && manager!=null){
								int dl_size=session_download/10240;
								float dl_mb=(float)dl_size/100;
								nb.setContentText(getString(R.string.tile_txt_sent)+": "+session_counter+"  "+getString(R.string.tile_txt_dl)+": "+dl_mb+"MB");
								manager.notify(12, nb.build());
							}
							Intent i=new Intent("co.bh.rekcuf.sniffer");
							i.putExtra("stat",Integer.toString(stat_int));
							i.putExtra("domain",domain);
							sendBroadcast(i);
						}
					}
				}else{
					setTileStat(false);
					srvStop();
				}
			}
		}

	}

	public int send_http_request(String str){
		int responseCode=-1;
		//String content="";
		String last_timeout=db1.se1("select v from data where k='last_timeout';");
		int timeout=Integer.parseInt(last_timeout);
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
			Log.println(Log.ERROR,"","--------> "+http_status);

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
		return responseCode;
	}

}
