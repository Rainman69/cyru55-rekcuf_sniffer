package co.bh.rekcuf.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


@RequiresApi(api=Build.VERSION_CODES.N)
public class bgTile extends TileService{

	public static boolean bgTile_start=false;
	ArrayList<Thread> T = new ArrayList<Thread>();

	@Override
	public void onTileAdded() {
		setTileStat(false);
	}

	@Override
	public void onClick() {
		super.onClick();
		Tile tile = getQsTile();
		tileSrv(tile.getState() == Tile.STATE_INACTIVE);
	}

	public void setTileStat(boolean stat){
		Tile tile = getQsTile();
		tile.setState(stat?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
		tile.updateTile();
	}

	public void tileSrv(boolean turn){
		boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
		if(net_stat){
			SQLite db1=new SQLite(bgTile.this);// init and create tables
			//try{one.switch_stat=turn;}catch(Exception e){}
			//db1.exe("update data set v='"+(turn?"1":"0")+"' where k='last_switch_stat';");
			bgTile_start=turn;
			setTileStat(turn);
			if(turn){
				srvStart();
			}else{
				srvStop();
			}
		}else {
			Toast.makeText(getApplication(),R.string.run_tile_toast_turnon,Toast.LENGTH_LONG).show();
			if(bgTile_start){
				srvStop();
			}
		}
	}

	public void srvStart(){
		String last_conc=SQLite.se1("select v from data where k='last_conc';");
		String last_notif=SQLite.se1("select v from data where k='last_notif';");
		int conc=Integer.parseInt(last_conc);
		int notif=Integer.parseInt(last_notif);
		if(notif>0){
			try{
				if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
					startNotif();
				}else{
					startForeground(1,new Notification());
				}
			}catch(Exception e){}
		}
		for(int i=0;i<conc;i++){
			Thread t = new Thread(new TileRunner(),"Runner"+i);
			T.add(t);
			t.start();
		}
	}

	public void srvStop(){
		for (Thread t : T) {
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

	class TileRunner implements Runnable{
		public void run(){
			while(bgTile_start){
				//String last_net_stat=SQLite.se1("select v from data where k='last_net_stat';");
				//if(!last_net_stat.equals("0")){
				boolean net_stat=NetworkUtil.isConnected(getApplicationContext());
				if(net_stat){
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
		String last_timeout=SQLite.se1("select v from data where k='last_timeout';");
		int timeout=Integer.parseInt(last_timeout);
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
