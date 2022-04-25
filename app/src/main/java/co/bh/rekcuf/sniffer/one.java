package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class one extends AppCompatActivity{

	public Handler handler1=new Handler();
	boolean stat=false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.one);

		ToggleButton netstat=findViewById(R.id.netstat);
		ToggleButton togglev1=findViewById(R.id.togglev1);
		EditText num1=findViewById(R.id.num1);
		Switch switch1=findViewById(R.id.switch1);
		Button button1=findViewById(R.id.button1);
		//Toast.makeText(one.this,"startService",Toast.LENGTH_SHORT).show();

		NetworkChangeReceiver.setToggle(netstat);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			NetworkChangeReceiver.registerNetworkCallback(this);

		new Timer().scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				stat=NetworkUtil.isConnected(getApplicationContext());
				netstat.setChecked(stat);
				boolean srv_stat=isServiceRunning(bgService.class);
				handler1.post(new Runnable(){
					@Override
					public void run(){
						togglev1.setChecked(srv_stat);
					}
				});
			}
		},0,500);

		button1.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				boolean stat=isServiceRunning(bgService.class);
				String x=Boolean.toString(stat);
				Toast.makeText(one.this,"stat: "+x,Toast.LENGTH_SHORT).show();
			}
		});

		num1.setOnKeyListener(new View.OnKeyListener(){
			@Override
			public boolean onKey(View view,int i,KeyEvent keyEvent){
				if(keyEvent.getAction()==KeyEvent.ACTION_UP){
					int num=get_conc();
					if(num>0&&num<17){
						//num1.setBackgroundColor(Color.TRANSPARENT);
						num1.getBackground().setColorFilter(getResources().getColor(R.color.teal_200),PorterDuff.Mode.SRC_IN);
					}else{
						//num1.setBackgroundColor(Color.parseColor("#FFAAAA"));
						num1.getBackground().setColorFilter(Color.RED,PorterDuff.Mode.SRC_IN);
					}
				}
				return false;
			}
		});

		switch1.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				Intent srv=new Intent(getApplication(),bgService.class);
				if(switch1.isChecked()){
					if(stat){
						num1.setEnabled(false);
						startService(srv);
						startThread(view);
					}else{
						Toast.makeText(one.this,"Internet is not available",Toast.LENGTH_SHORT).show();
						switch1.setChecked(false);
					}
				}else{
					num1.setEnabled(true);
					stopService(srv);
				}
			}
		});

	}

	public int get_conc(){
		EditText num1=findViewById(R.id.num1);
		String txt=num1.getText().toString();
		if(txt.length()>0){
			int num=Integer.parseInt(txt);
			if(num>0){
				return num;
			}else
				return 0;
		}else
			return 0;
	}

	private boolean isServiceRunning(Class<?> serviceClass){
		ActivityManager manager=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)){
			if(serviceClass.getName().equals(service.service.getClassName())){
				return true;
			}
		}
		return false;
	}

	public String url2str(String targetURL){
		try{
			URL url=new URL(targetURL);
			BufferedReader in=new BufferedReader(new InputStreamReader(url.openStream()));
			String line;
			String res="";
			while((line=in.readLine())!=null){
				res=res.concat(line+"\n");
			}
			in.close();
			return res;
		}catch(IOException e){//todo solve android4 ssl1.3 error stackoverflow.com/a/30302235
			return "";
		}
	}

	public void startThread(View view){
		int num=get_conc();

        /*for (int i=0; i<num; i++) {
            new Thread(new Runner(), "Runner"+num).start();
        }*/
		new Thread(new Runner(),"Runner1").start();
	}

	public void stopThread(View view){
	}

	class Runner implements Runnable{
		public void run(){
			String raw=url2str("https://9k.gg/rs2");
			handler1.post(new Runnable(){
				@Override
				public void run(){
					LinearLayout ll=findViewById(R.id.logger);
					ll.removeAllViews();
					ll.invalidate();
					TextView txt=new TextView(one.this);
					String timeStamp=new SimpleDateFormat("HH:mm:ss").format(new Date());
					txt.setText(timeStamp+"\n"+raw);
					ll.addView(txt);
				}
			});
		}
	}


	class bgThread extends Thread{
		int conc=4;
		bgThread(int conc){
			this.conc=conc;
		}
		@Override
		public void run(){
			//Toast.makeText(getApplicationContext(),"onStartCommand",Toast.LENGTH_SHORT).show();
            /*handler1.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(one.this,"bgThread.run()",Toast.LENGTH_SHORT).show();
                }
            });
            Connection conn = null;
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:res/raw/hosts.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("select host from host where rowid=(abs(random()) % 17890);");
            //rs.next();
            String host = rs.getString("name");
            rs.close();
            conn.close();
            handler1.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(one.this, "host: " + host, Toast.LENGTH_SHORT).show();
                }
            });*/

		}
	}

	public int Requester(String str){
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
				Toast.makeText(one.this,"content len: "+len,Toast.LENGTH_SHORT).show();
			}
		}catch(Exception ex){
			handler1.post(new Runnable(){
				@Override
				public void run(){
					Toast.makeText(one.this,"Requester Exception",Toast.LENGTH_SHORT).show();
				}
			});
		}
		return responseCode;

	}

}
