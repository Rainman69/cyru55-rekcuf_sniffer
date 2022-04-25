package co.bh.rekcuf.sniffer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class bgService extends Service{

	public static final String pn = "co.bh.rekcuf.sniffer";

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
		int num=one.conc;
        for (int i=0; i<num; i++) {
            new Thread(new Runner(), "Runner"+num).start();
        }
		new Thread(new Runner(),"Runner1").start();
	}

	class Runner implements Runnable{
		public void run(){
			while(one.switch1){
				Cursor res=SQLite.sel("select host from host order by random() limit 1;");
				if(res.moveToNext()){
					String domain=res.getString(0);
					//toast(domain);
					if(domain.length()>3){
						String url="https://"+domain+"/?";
						int stat_int=send_http_request(url);
						String stat_str=Integer.toString(stat_int);
						if(stat_str.equals("-1"))
							stat_str="000";
						final String stat=stat_str;
						one.handler1.post(new Runnable(){
							@Override
							public void run(){
								Intent i=new Intent("co.bh.rekcuf.sniffer");
								i.putExtra("stat",stat);
								i.putExtra("domain",domain);
								sendBroadcast(i);
							}
						});
					}
				}
			}
		}
	}

	public void toast(String str){
		one.handler1.post(new Runnable(){
			@Override
			public void run(){
				Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
			}
		});
	}
	public int send_http_request(String str){
		int responseCode=-1;
		//String content="";
		try{
			URL url=new URL(str);
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			urlConn.setConnectTimeout(3000);
			urlConn.setReadTimeout(3000);
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
