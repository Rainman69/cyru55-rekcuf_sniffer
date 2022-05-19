package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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
	public static int timeout=5000;
	public static boolean switch_stat=false;
	public static boolean notif=true;
	public static boolean stat=false;
	public int db_count=0;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.one);

		ToggleButton netstat=findViewById(R.id.netstat);
		ToggleButton togglev1=findViewById(R.id.togglev1);
		EditText inp1=findViewById(R.id.inp1);
		EditText inp4=findViewById(R.id.inp4);
		EditText inp5=findViewById(R.id.inp5);
		SwitchMaterial switch1=findViewById(R.id.switch1);
		CheckBox checkbox1=findViewById(R.id.checkbox1);
		LinearLayout ll=findViewById(R.id.logger);

		//Toast.makeText(one.this,"startService",Toast.LENGTH_SHORT).show();

		NetworkChangeReceiver.setToggle(netstat);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			NetworkChangeReceiver.registerNetworkCallback(this);

		new Timer().scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				conc=get_conc();
				timeout=get_timeout();
				notif=checkbox1.isChecked();
				stat=NetworkUtil.isConnected(getApplicationContext());
				netstat.setChecked(stat);
				handler1.post(new Runnable(){
					@Override
					public void run(){
						togglev1.setChecked(isServiceRunning(bgService.class));
						if(!stat&&switch1.isChecked()){
							switch1.setChecked(false);
							service(false);
						}
						EditText inp3=findViewById(R.id.inp3);
						String sent_total=SQLite.se1("select v from data where k='sent_total';");
						if(sent_total.length()>0){
							inp3.setText(sent_total);
						}
					}
				});
			}
		},0,600);

		SQLite db1=new SQLite(one.this);// init and create tables
		String res=db1.se1("select count(*) as x from host;");
		if(res.length()>0){
			db_count=Integer.parseInt(res);
			if(db_count>0){
				inp1.setText(Integer.toString(db_count));
				TextView txtv=new TextView(getApplicationContext());
				txtv.setText("your DataBase have "+db_count+" domains\nLets Go\n");
				ll.addView(txtv);
			}else{
				new java.util.Timer().schedule(new java.util.TimerTask(){
					public void run(){
						if(stat){
							handler1.post(new Runnable(){
								@Override
								public void run(){
									ll.removeAllViews();
									ll.invalidate();
									TextView txtv=new TextView(getApplicationContext());
									txtv.setText("DataBase is now Updating ...");
									ll.addView(txtv);
								}
							});
							new Thread(new Runnable(){
								@Override
								public void run(){
									updatedb(getString(R.string.update_url));
								}
							}).start();
						}else{
							handler1.post(new Runnable(){
								@Override
								public void run(){
									Toast.makeText(one.this,"Turn On Internet",Toast.LENGTH_SHORT).show();
								}
							});
							finish();
						}

					}
				},1000);
			}
		}

		switch1.setChecked(switch_stat);
		if(switch_stat){
			inp4.setEnabled(false);
			inp5.setEnabled(false);
			checkbox1.setEnabled(false);
		}
		String last_conc=SQLite.se1("select v from data where k='last_conc';");
		inp4.setText(last_conc);
		String last_timeout=SQLite.se1("select v from data where k='last_timeout';");
		int last_timeout_int=Integer.parseInt(last_timeout);
		if(last_timeout_int>0){
			last_timeout_int=Math.floorDiv(last_timeout_int,1000);
			inp5.setText(Integer.toString(last_timeout_int));
		}
		String last_notif=SQLite.se1("select v from data where k='last_notif';");
		checkbox1.setChecked(last_notif.equals("1"));

		inp4.setOnKeyListener(new View.OnKeyListener(){
			@Override
			public boolean onKey(View view,int i,KeyEvent keyEvent){
				if(keyEvent.getAction()==KeyEvent.ACTION_UP){
					conc=get_conc();
					if(conc>0&&conc<17){
						//inp4.setBackgroundColor(Color.TRANSPARENT);
						inp4.getBackground().setColorFilter(getResources().getColor(R.color.teal_200),PorterDuff.Mode.SRC_IN);
					}else{
						//inp4.setBackgroundColor(Color.parseColor("#FFAAAA"));
						inp4.getBackground().setColorFilter(Color.RED,PorterDuff.Mode.SRC_IN);
					}
				}
				return false;
			}
		});

		switch1.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				switch_stat=false;
				if(switch1.isChecked()){
					if(stat){
						conc=get_conc();
						if(conc>0&&conc<33){
							int timeout=get_timeout();
							if(timeout>999&&timeout<20001){
								if(db_count>0){
									SQLite.exe("update data set v='"+conc+"' where k='last_conc';");
									SQLite.exe("update data set v='"+timeout+"' where k='last_timeout';");
									SQLite.exe("update data set v='"+(checkbox1.isChecked()?"1":"0")+"' where k='last_notif';");
									service(true);
								}else{
									Toast.makeText(one.this,"Wait until DataBase become updated",Toast.LENGTH_SHORT).show();
									switch1.setChecked(false);
								}
							}else{
								Toast.makeText(one.this,"Set Timeout 1 ~ 20",Toast.LENGTH_SHORT).show();
								switch1.setChecked(false);
							}
						}else{
							Toast.makeText(one.this,"Set concurrent number 1 ~ 16",Toast.LENGTH_SHORT).show();
							switch1.setChecked(false);
						}
					}else{
						Toast.makeText(one.this,"Internet is not available",Toast.LENGTH_SHORT).show();
						switch1.setChecked(false);
					}
				}else{
					service(false);
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
	@Override
	public void onDestroy(){
		super.onDestroy();
		//db1.close();
	}

	public boolean isServiceRunning(Class<?> serviceClass){
		ActivityManager manager=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)){
			if(serviceClass.getName().equals(service.service.getClassName())){
				return true;
			}
		}
		return false;
	}

	public int get_conc(){
		EditText inp4=findViewById(R.id.inp4);
		String txt=inp4.getText().toString();
		if(txt.length()>0){
			int num=Integer.parseInt(txt);
			if(num>0){
				return num;
			}
			return 0;
		}
		return 0;
	}

	public int get_timeout(){
		EditText inp5=findViewById(R.id.inp5);
		String txt=inp5.getText().toString();
		if(txt.length()>0){
			int num=Integer.parseInt(txt);
			if(num>0&&num<21){
				return num*1000;
			}
			return 5000;
		}
		return 5000;
	}

	public String get_ts(){
		return new SimpleDateFormat("HH:mm:ss").format(new Date());
	}

	public void service(boolean turn){
		switch_stat=turn;
		EditText inp4=findViewById(R.id.inp4);
		EditText inp5=findViewById(R.id.inp5);
		CheckBox checkbox1=findViewById(R.id.checkbox1);
		Intent srv=new Intent(getApplication(),bgService.class);
		inp4.setEnabled(!turn);
		inp5.setEnabled(!turn);
		checkbox1.setEnabled(!turn);
		if(turn){
			startService(srv);
		}else{
			stopService(srv);
		}
		LinearLayout ll=findViewById(R.id.logger);
		TextView txtv=new TextView(getApplicationContext());
		txtv.setText(get_ts()+"  ------------------ "+(turn?"Start":"Stop"));
		ll.addView(txtv);
	}

	public void updatedb(String targetURL){
		String line;
		boolean res;
		int i=0;
		try{
			URL url=new URL(targetURL);
			BufferedReader in=new BufferedReader(new InputStreamReader(url.openStream()));
			while((line=in.readLine())!=null){
				++i;
				if(line.length()>3){
					do{// repeat insert if db file locked temporary
						res=SQLite.ins("host",new String[]{"domain",line});
					}while(res==false);
				}
			}
			in.close();
			db_count=i;
			handler1.post(new Runnable(){
				@Override
				public void run(){
					EditText inp1=findViewById(R.id.inp1);
					inp1.setText(Integer.toString(db_count));
					LinearLayout ll=findViewById(R.id.logger);
					TextView txtv=new TextView(getApplicationContext());
					txtv.setText("DataBase Updated Successfuly\nyour DataBase have "+db_count+" domains\nLets Go");
					ll.addView(txtv);
				}
			});
		}catch(IOException e){//todo solve android4 ssl1.3 error stackoverflow.com/a/30302235
			e.printStackTrace();
		}
	}

	public BroadcastReceiver rcv=new BroadcastReceiver(){
		@Override
		public void onReceive(Context context,Intent intent){
			Bundle bundle=intent.getExtras();
			if(bundle!=null){
				EditText inp2=findViewById(R.id.inp2);
				String inp2_str=inp2.getText().toString();
				int inp2_int=inp2_str.length()>0?Integer.parseInt(inp2_str):0;
				inp2.setText((inp2_int+1)+"");
				LinearLayout ll=findViewById(R.id.logger);
				int ll_count=ll.getChildCount();
				if(ll_count>40){
					ll.removeView(ll.getChildAt(0));
				}
				//ll.removeAllViews();
				//ll.invalidate();
				ScrollView sv=findViewById(R.id.logger_parent);
				String ts_str=get_ts();
				String stat=bundle.getString("stat");
				String domain=bundle.getString("domain");
				String stat_str=stat.equals("-1")?"000 \u00a0 ×":stat+" \u00a0 <";
				String log=ts_str+" \u00a0 - \u00a0 "+stat_str+" \u00a0 "+domain+"\n";
				TextView txtv=new TextView(getApplicationContext());
				txtv.setSingleLine(true);
				//txtv.setMaxLines(1);
				txtv.setText(log);
				ll.addView(txtv);
				sv.fullScroll(ScrollView.FOCUS_DOWN);
			}
		}
	};

}
