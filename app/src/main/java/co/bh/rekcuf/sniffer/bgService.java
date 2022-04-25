package co.bh.rekcuf.sniffer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
        /*for (int i=0; i<num; i++) {
            new Thread(new Runner(), "Runner"+num).start();
        }*/
		new Thread(new Runner(),"Runner1").start();
	}

	class Runner implements Runnable{
		public void run(){
			String raw="x";
			one.handler1.post(new Runnable(){
				@Override
				public void run(){
					Intent i = new Intent("co.bh.rekcuf.sniffer");
					i.putExtra("raw",raw);
					sendBroadcast(i);
				}
			});

		}
	}

	public int url2httpcode(String str){
		InputStream in=null;
		int responseCode=-1;
		String content="";
		try{
			URL url=new URL(str);
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			urlConn.setConnectTimeout(9000);
			urlConn.setReadTimeout(9000);
			BufferedReader br=new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			//urlConn.setAllowUserInteraction(false);
			//urlConn.setDoInput(true);
			urlConn.connect();
			responseCode=urlConn.getResponseCode();
			if(responseCode==HttpURLConnection.HTTP_OK){
				String q;
				do{
					q=br.readLine();
					content+=q;
				}while(q!=null);
				br.close();
			}
			urlConn.disconnect();
			int len=content.length();
			if(len>0){
				Toast.makeText(getApplicationContext(),"content len: "+len,Toast.LENGTH_SHORT).show();
			}
		}catch(Exception ex){
		}
		return responseCode;
	}

}
