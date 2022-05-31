package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
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
	public static boolean net_stat=false;
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
		TextView text_r7_1=findViewById(R.id.text_r7_1);
		Button two_btn_auto=findViewById(R.id.two_btn_auto);
		Button two_btn_add=findViewById(R.id.two_btn_add);
		ImageButton one_add_btn=findViewById(R.id.one_add_btn);

		NetworkChangeReceiver.setToggle(netstat);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			NetworkChangeReceiver.registerNetworkCallback(this);

		new Timer().scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				conc=get_conc();
				timeout=get_timeout();
				notif=checkbox1.isChecked();
				net_stat=NetworkUtil.isConnected(getApplicationContext());
				handler1.post(new Runnable(){
					@Override
					public void run(){
						netstat.setChecked(net_stat);
						togglev1.setChecked(isServiceRunning(bgService.class));
						if(!net_stat&&switch1.isChecked()){
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
			TextView txtv=new TextView(getApplicationContext());
			txtv.setText(R.string.run_one_log_dev);
			ll.addView(txtv);
			db_count=Integer.parseInt(res);
			if(db_count>0){
				if(db_count<400){
					alert_box(getString(R.string.run_one_alert_notenough));
				}
				inp1.setText(Integer.toString(db_count));
				txtv=new TextView(getApplicationContext());
				txtv.setText(getString(R.string.run_one_log_updated1)+db_count+getString(R.string.run_one_log_updated2));
				ll.addView(txtv);
			}else{
				do_updatedb();
			}
		}

		String last_switch_stat=db1.se1("select v from data where k='last_switch_stat';");
		int switch_stat_int=Integer.parseInt(last_switch_stat);
		switch_stat=switch_stat_int>0?true:false;
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
		String last_net_stat=SQLite.se1("select v from data where k='last_net_stat';");
		netstat.setChecked(last_net_stat.equals("0")?false:true);

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
					if(net_stat){
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
									Toast.makeText(getApplicationContext(),R.string.run_one_toast_waitupdating,Toast.LENGTH_SHORT).show();
									switch1.setChecked(false);
								}
							}else{
								Toast.makeText(getApplicationContext(),R.string.run_one_toast_badtimeout,Toast.LENGTH_SHORT).show();
								switch1.setChecked(false);
							}
						}else{
							Toast.makeText(getApplicationContext(),R.string.run_one_toast_badconc,Toast.LENGTH_SHORT).show();
							switch1.setChecked(false);
						}
					}else{
						Toast.makeText(getApplicationContext(),R.string.run_one_toast_nointernet,Toast.LENGTH_SHORT).show();
						switch1.setChecked(false);
					}
				}else{
					service(false);
				}
			}
		});

		text_r7_1.setOnLongClickListener(new View.OnLongClickListener(){
			@Override
			public boolean onLongClick(View view){
				String ved=getString(R.string.ved).replace("E","@").replace("ver","c")+"r";
				Toast.makeText(one.this,ved+"u"+(61-6),Toast.LENGTH_LONG).show();
				return false;
			}
		});

		one_add_btn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				findViewById(R.id.two_layout).setVisibility(View.VISIBLE);
			}
		});

		two_btn_auto.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				if(net_stat){
					handler1.post(new Runnable(){
						@Override
						public void run(){
							LinearLayout ll=findViewById(R.id.logger);
							ll.removeAllViews();
							ll.invalidate();
							TextView txtv=new TextView(getApplicationContext());
							txtv.setText(R.string.run_one_log_updating);
							ll.addView(txtv);
							findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
						}
					});
					new Thread(new Runnable(){
						@Override
						public void run(){
							updatedb(getString(R.string.update_url));
						}
					}).start();
				}else{
					Toast.makeText(getApplication(),R.string.run_one_toast_turnon,Toast.LENGTH_LONG).show();
					finish();
				}
			}
		});

		two_btn_add.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				EditText two_paste_text=findViewById(R.id.two_paste_text);
				String paste=two_paste_text.getText().toString();
				String raw2=paste.replaceAll("\\s+","\n");
				String raw=raw2.replaceAll("[^a-zA-Z0-9\\-\\.\\n:/]+","");
				int count_lines=raw.length()-raw.replace("\n","").length();
				int raw_len=raw.length();
				if(count_lines<1){
					if(raw_len==0){
						findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
					}
					if(raw_len>14&&raw.matches("^((http|https)://).+/.+")){
						if(net_stat){
							handler1.post(new Runnable(){
								@Override
								public void run(){
									LinearLayout ll=findViewById(R.id.logger);
									ll.removeAllViews();
									ll.invalidate();
									TextView txtv=new TextView(getApplicationContext());
									txtv.setText(getString(R.string.run_one_log_updating)+getString(R.string.run_one_log_updating_customurl)+raw);
									ll.addView(txtv);
									Toast.makeText(getApplication(),R.string.run_one_toast_wait4customurl,Toast.LENGTH_LONG).show();
									two_paste_text.setText("");
									findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
								}
							});
							new Thread(new Runnable(){
								@Override
								public void run(){
									updatedb(raw);
								}
							}).start();
						}else{
							Toast.makeText(getApplication(),R.string.run_one_toast_turnon,Toast.LENGTH_LONG).show();
							finish();
						}
					}
				}else if(count_lines>99&&raw_len>600){
					String domain3=raw.replaceAll("^((http|https)://)","");
					String domain2=domain3.replaceAll("/.+","");
					String domain1=domain2.replaceAll("[:/]","");
					String[] lines=domain1.split("\n");
					int i=0;
					boolean res;
					for(String line:lines){
						if(line.length()>3){
							if(!line.matches("(\\.|\\-){2,}")){
								if(!line.matches("^[\\.\\-]|[\\.\\-]$")){
									if(line.matches("^[a-zA-Z0-9\\-\\.]{2,32}\\.[a-zA-Z]{2,9}$")){
										++i;
										int j=9;
										do{// repeat insert if db file locked temporary
											res=SQLite.ins("host",new String[]{"domain",line.toLowerCase()});
											if(res==false) --j;
										}while(res==false&&j>0);// ignore insert after max try
									}
								}
							}
						}
					}
					db_count=i;
					handler1.post(new Runnable(){
						@Override
						public void run(){
							inp1.setText(Integer.toString(db_count));
							TextView txtv=new TextView(getApplicationContext());
							txtv.setText(getString(R.string.run_one_log_updated_now1)+db_count+getString(R.string.run_one_log_updated_now2));
							ll.addView(txtv);
							two_paste_text.setText("");
							findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
						}
					});
				}else{
					alert_box(getString(R.string.run_two_alert_addlistonly));
				}
			}
		});

	}
	@Override
	protected void onResume(){
		super.onResume();
		registerReceiver(rcv,new IntentFilter("co.bh.rekcuf.sniffer"));
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

	public void alert_box(String str){
		new AlertDialog.Builder(one.this)
			//.setTitle("Delete entry")
			.setMessage(str)
			.setPositiveButton(android.R.string.ok,null)
			.setCancelable(false)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();
	}

	public void do_updatedb(){
		AlertDialog.Builder alert = new AlertDialog.Builder(one.this);
		alert.setTitle(R.string.run_one_confirm_empty1);
		alert.setMessage(R.string.run_one_confirm_empty2);
		alert.setCancelable(false);
		alert.setPositiveButton(R.string.run_one_confirm_empty_auto, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(net_stat){
					dialog.dismiss();
					handler1.post(new Runnable(){
						@Override
						public void run(){
							LinearLayout ll=findViewById(R.id.logger);
							ll.removeAllViews();
							ll.invalidate();
							TextView txtv=new TextView(getApplicationContext());
							txtv.setText(R.string.run_one_log_updating);
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
					Toast.makeText(getApplication(),R.string.run_one_toast_turnon,Toast.LENGTH_LONG).show();
					finish();
				}
			}
		});
		alert.setNegativeButton(R.string.run_one_confirm_empty_manual, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				RelativeLayout two_layout=findViewById(R.id.two_layout);
				two_layout.setVisibility(View.VISIBLE);
				EditText two_paste_text=findViewById(R.id.two_paste_text);
				two_paste_text.setHorizontallyScrolling(true);
			}
		});
		AlertDialog dialog=alert.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
	}

	public void service(boolean turn){
		switch_stat=turn;
		SQLite.exe("update data set v='"+(turn?"1":"0")+"' where k='last_switch_stat';");
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
		txtv.setText(get_ts()+"  ------------------ "+(turn?getString(R.string.run_one_log_line_start):getString(R.string.run_one_log_line_stop)));
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
					int j=9;
					do{// repeat insert if db file locked temporary
						res=SQLite.ins("host",new String[]{"domain",line});
						if(res==false) --j;
					}while(res==false&&j>0);// ignore insert after max try
				}
			}
			in.close();
			db_count=i;
		}catch(IOException e){//todo solve android4 ssl1.3 error stackoverflow.com/a/30302235
			e.printStackTrace();
		}
		handler1.post(new Runnable(){
			@Override
			public void run(){
				EditText inp1=findViewById(R.id.inp1);
				inp1.setText(Integer.toString(db_count));
				LinearLayout ll=findViewById(R.id.logger);
				TextView txtv=new TextView(getApplicationContext());
				txtv.setText(getString(R.string.run_one_log_updated_now1)+db_count+getString(R.string.run_one_log_updated_now2));
				ll.addView(txtv);
			}
		});
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
				String stat_str=stat.equals("-1")?"000 \u00A0 ×":stat+" \u00A0 <";
				String log=ts_str+" \u00A0 - \u00A0 "+stat_str+" \u00A0 "+domain+"\n";
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
