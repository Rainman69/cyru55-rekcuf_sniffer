package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class one extends AppCompatActivity{

	public static Handler handler1=new Handler();
	public static int conc=0;
	public boolean stat=false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.one);

		ToggleButton netstat=findViewById(R.id.netstat);
		ToggleButton togglev1=findViewById(R.id.togglev1);
		EditText num1=findViewById(R.id.num1);
		EditText num2=findViewById(R.id.num2);
		EditText num3=findViewById(R.id.num3);
		SwitchMaterial switch1=findViewById(R.id.switch1);
		Button button1=findViewById(R.id.button1);
		LinearLayout ll=findViewById(R.id.logger);
		//Toast.makeText(one.this,"startService",Toast.LENGTH_SHORT).show();

		SQLite db1=new SQLite(this);// init and create table
		Cursor res=db1.sel("select count(*) as x from "+db1.tablename+";");
		if(res.moveToNext()){
			String counter=res.getString(0);
			int count=Integer.parseInt(counter);
			if(count>0){
				num2.setText(Integer.toString(count));
			}else{
				ll.removeAllViews();
				ll.invalidate();
				TextView txtv=new TextView(getApplicationContext());
				txtv.setText("DataBase is now Updating ...");
				ll.addView(txtv);
				new Thread(new Runnable(){
					@Override
					public void run(){
						updatedb(getString(R.string.update_url));
					}
				}).start();
			}
		}else{
			Toast.makeText(one.this,"have not next result",Toast.LENGTH_SHORT).show();
		}

		NetworkChangeReceiver.setToggle(netstat);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			NetworkChangeReceiver.registerNetworkCallback(this);

		new Timer().scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				conc=get_conc();
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
					int conc=get_conc();
					if(conc>0&&conc<17){
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
						int conc=get_conc();
						if(conc>0&&conc<17){
							num1.setEnabled(false);
							startService(srv);
						}else{
							Toast.makeText(one.this,"Set concurrent number 1 ~ 16",Toast.LENGTH_SHORT).show();
							switch1.setChecked(false);
						}
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
	@Override
	protected void onResume(){
		super.onResume();
		registerReceiver(rcv,new IntentFilter(bgService.pn));
	}
	@Override
	protected void onPause(){
		super.onPause();
		unregisterReceiver(rcv);
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

	public int get_conc(){
		EditText num1=findViewById(R.id.num1);
		String txt=num1.getText().toString();
		if(txt.length()>0){
			int num=Integer.parseInt(txt);
			if(num>0){
				return num;
			}
			return 0;
		}
		return 0;
	}

	public void updatedb(String targetURL){
		String line;
		boolean res;
		int i=0;
		SQLite db2=new SQLite(one.this);
		try{
			URL url=new URL(targetURL);
			BufferedReader in=new BufferedReader(new InputStreamReader(url.openStream()));
			while((line=in.readLine())!=null){
				++i;
				if(line.length()>3){
					do{// repeat insert if db file locked temporary
						res=db2.ins(line);
					}while(res==false);
				}
			}
			in.close();
			int finalI=i;
			handler1.post(new Runnable(){
				@Override
				public void run(){
					EditText num2=findViewById(R.id.num2);
					num2.setText(Integer.toString(finalI));
					LinearLayout ll=findViewById(R.id.logger);
					TextView txtv=new TextView(getApplicationContext());
					txtv.setText("DataBase Updated Successfuly\nyour DataBase have "+finalI+" domains\nLets Go");
					ll.addView(txtv);
				}
			});
		}catch(IOException e){//todo solve android4 ssl1.3 error stackoverflow.com/a/30302235
			e.printStackTrace();
		}
	}

	private BroadcastReceiver rcv=new BroadcastReceiver(){
		@Override
		public void onReceive(Context context,Intent intent){
			Bundle bundle=intent.getExtras();
			if(bundle!=null){
				String raw=bundle.getString("raw");
				LinearLayout ll=findViewById(R.id.logger);
				ll.removeAllViews();
				ll.invalidate();
				TextView txtv=new TextView(getApplicationContext());
				String timeStamp=new SimpleDateFormat("HH:mm:ss").format(new Date());
				txtv.setText(timeStamp+"\n"+raw);
				ll.addView(txtv);
			}
		}
	};
}
