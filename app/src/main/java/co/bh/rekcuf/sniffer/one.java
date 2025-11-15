package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class one extends AppCompatActivity{

	public String app_pack_name="co.bh.rekcuf.sniffer";
	public SQLite db1=null;
	public static int conc=0;
	public static int timeout=5000;
	public static boolean notif=true;
	public static boolean net_stat=false;
	public int db_count=0;
	public static Handler handler1 = new Handler();

	@Override protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Log.e("__A","onCreate");
		setContentView(R.layout.one);

		if(db1==null){
			Log.e("__L","onCreate: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

		CheckBox checkbox1=findViewById(R.id.checkbox1);
		ToggleButton netstat=findViewById(R.id.netstat);
		ToggleButton togglev1=findViewById(R.id.togglev1);
		SwitchMaterial switch1=findViewById(R.id.switch1);
		LinearLayout ll=findViewById(R.id.logger);

		NetworkChangeReceiver.setToggle(netstat);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			NetworkChangeReceiver.registerNetworkCallback(this);
		}

		String res1=SQLite.se1("select count(*) as x from host where valid>0;");
		if(res1.length()>0){
			db_count=Integer.parseInt(res1);
			TextView txtv1=new TextView(getApplicationContext());
			txtv1.setText(R.string.run_one_log_dev);
			ll.addView(txtv1);
			if(db_count>0){
				if(db_count<400){
					alert_box(getString(R.string.run_one_alert_notenough));
				}
				TextView txtv2=new TextView(getApplicationContext());
				txtv2.setText(getString(R.string.run_one_log_updated1)+db_count+getString(R.string.run_one_log_updated2));
				ll.addView(txtv2);
			}else{
				res1=SQLite.se1("select count(*) as x from host;");
				db_count=Integer.parseInt(res1);
				if(db_count>0){// reset `valid` column at all rows
					SQLite.exe("update host set valid=5;");
				}else{
					prompt_updatedb();
				}
			}
		}

		if(Build.VERSION.SDK_INT>=32){// Android 13+
			ActivityResultLauncher<String> launcher=registerForActivityResult(
				new ActivityResultContracts.RequestPermission(),isGranted -> {}
			);
			launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
		}

		new Timer().schedule(new TimerTask(){@Override public void run(){
			conc=get_conc();
			timeout=get_timeout();
			notif=checkbox1.isChecked();
			net_stat=NetworkUtil.isConnected(getApplicationContext());
			runOnUiThread(()->{
				netstat.setChecked(net_stat);
				togglev1.setChecked(isServiceRunning(bgService.class));
				if(!net_stat&&switch1.isChecked()){
					switch1.setChecked(false);
					service(false);
				}
				if(net_stat&&mem("switch").equals("0")&&mem("switch_by_user").equals("1")){
					switch1.setChecked(true);
					service(true);
				}
				//if(sent_total.equals("")) sent_total=SQLite.se1("select v from data where k='sent_total';");
				((EditText)findViewById(R.id.inp3)).setText(mem("sent_total"));
			});
		}},0,500);


	}
	@Override public void onStart(){
		super.onStart();
		Log.e("__A","onStart");

		if(db1==null){
			Log.e("__L","onCreate: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

	}
	@Override public void onRestart(){
		super.onRestart();
		Log.e("__A","onRestart");

		if(db1==null){
			Log.e("__L","onRestart: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

	}
	@Override protected void onResume(){
		super.onResume();
		Log.e("__A","onResume");

		if(db1==null){
			Log.e("__L","onResume: Try Open DB");
			try{
				db1=new SQLite(getApplicationContext());
			}catch(Exception e){e.printStackTrace();}
		}

		ToggleButton netstat=findViewById(R.id.netstat);
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

		String res1=SQLite.se1("select count(*) as x from host where valid>0;");
		if(res1.length()>0){
			db_count=Integer.parseInt(res1);
			inp1.setText(Integer.toString(db_count));
			if(db_count<1){
				if(!isIgnoreBatteryOptimize()){
					String res2=SQLite.se1("select v from data where k='ask_ignore_battery';");
					int ask_ignore_battery=Integer.parseInt(res2);
					if(ask_ignore_battery>0){
						SQLite.exe("update data set v='"+(ask_ignore_battery-1)+"' where k='ask_ignore_battery';");
						reqIgnoreBatteryOptimize();
					}
				}
			}
		}

		String last_switch_stat=SQLite.se1("select v from data where k='last_switch_stat';");
		int switch_stat_int=Integer.parseInt(last_switch_stat);
		mem("switch",switch_stat_int>0?"1":"0");
		switch1.setChecked(switch_stat_int>0);
		if(switch_stat_int>0){
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

		String sent_total=mem("sent_total");
		if(sent_total.equals(""))
			sent_total=SQLite.se1("select v from data where k='sent_total';");
		if(sent_total.length()>0){
			int sent_total_int=Integer.parseInt(sent_total);
			mem("sent_total",String.valueOf(sent_total_int));
			((EditText)findViewById(R.id.inp3)).setText(String.valueOf(sent_total_int));
		}

		String last_notif=SQLite.se1("select v from data where k='last_notif';");
		checkbox1.setChecked(last_notif.equals("1"));

		String last_net_stat=SQLite.se1("select v from data where k='last_net_stat';");
		netstat.setChecked(!last_net_stat.equals("0"));

		inp4.setOnKeyListener(new View.OnKeyListener(){
			@Override public boolean onKey(View view,int i,KeyEvent keyEvent){
				if(keyEvent.getAction()==KeyEvent.ACTION_UP){
					conc=get_conc();
					if(conc>0&&conc<17){
						//inp4.setBackgroundColor(Color.TRANSPARENT);
						inp4.getBackground().setColorFilter(getResources().getColor(R.color.teal_200),PorterDuff.Mode.SRC_IN);
						SQLite.exe("update data set v='"+conc+"' where k='last_conc';");
					}else{
						//inp4.setBackgroundColor(Color.parseColor("#FFAAAA"));
						inp4.getBackground().setColorFilter(Color.RED,PorterDuff.Mode.SRC_IN);
					}
				}
				return false;
			}
		});

		switch1.setOnClickListener(new View.OnClickListener(){
			@Override public void onClick(View view){
				mem("switch","0");
				if(switch1.isChecked()){
					if(net_stat){
						conc=get_conc();
						if(conc>0&&conc<33){
							int timeout=get_timeout();
							if(timeout>999&&timeout<12001){
								if(db_count>0){
									SQLite.exe("update data set v='"+conc+"' where k='last_conc';");
									SQLite.exe("update data set v='"+timeout+"' where k='last_timeout';");
									SQLite.exe("update data set v='"+(checkbox1.isChecked()?"1":"0")+"' where k='last_notif';");
									mem("switch_by_user","1");
									service(true);
								}else{
									toast_show(R.string.run_one_toast_updatefirst);
									switch1.setChecked(false);
								}
							}else{
								toast_show(R.string.run_one_toast_badtimeout);
								switch1.setChecked(false);
							}
						}else{
							toast_show(R.string.run_one_toast_badconc);
							switch1.setChecked(false);
						}
					}else{
						toast_show(R.string.run_one_toast_nointernet);
						switch1.setChecked(false);
					}
				}else{
					mem("switch_by_user","0");
					service(false);
				}
			}
		});

		text_r7_1.setOnLongClickListener(new View.OnLongClickListener(){
			@Override public boolean onLongClick(View view){
				String ved=getString(R.string.ved).replace("E","@").replace("ver","c")+"r";
				Toast.makeText(getApplicationContext(),ved+"u"+(61-6),Toast.LENGTH_LONG).show();
				return false;
			}
		});

		one_add_btn.setOnClickListener(new View.OnClickListener(){
			@Override public void onClick(View view){
				findViewById(R.id.two_layout).setVisibility(View.VISIBLE);
			}
		});

		if(db_count>999){
			two_btn_auto.setVisibility(View.INVISIBLE);
		}else{
			two_btn_auto.setOnClickListener(new View.OnClickListener(){
				@Override public void onClick(View view){
					if(net_stat){
						runOnUiThread(()->{
							LinearLayout ll=findViewById(R.id.logger);
							ll.removeAllViews();
							ll.invalidate();
							TextView txtv=new TextView(getApplicationContext());
							txtv.setText(R.string.run_one_log_updating);
							ll.addView(txtv);
							findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
						});
						new Thread(new Runnable(){@Override public void run(){
							updatedb(getString(R.string.update_url));
						}}).start();
					}else{
						toast_show(R.string.run_one_toast_turnon);
						finish();
					}
				}
			});
		}

		two_btn_add.setOnClickListener(new View.OnClickListener(){
			@Override public void onClick(View view){
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
							runOnUiThread(()->{
								LinearLayout ll=findViewById(R.id.logger);
								ll.removeAllViews();
								ll.invalidate();
								TextView txtv=new TextView(getApplicationContext());
								txtv.setText(getString(R.string.run_one_log_updating)+getString(R.string.run_one_log_updating_customurl)+raw);
								ll.addView(txtv);
								toast_show(R.string.run_one_toast_wait4customurl);
								two_paste_text.setText("");
								findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
							});
							new Thread(new Runnable(){@Override public void run(){
								updatedb(raw);
							}}).start();
						}else{
							toast_show(R.string.run_one_toast_turnon);
							finish();
						}
					}
				}else if(count_lines>99&&raw_len>600){
					String domain3=raw.replaceAll("^((http|https)://)","");
					String domain2=domain3.replaceAll("/.+","");
					String domain1=domain2.replaceAll("[:/]","");
					String[] lines=domain1.split("\n");
					int i=0;
					for(String line:lines){
						if(line.length()>3){
							if(!line.matches("(\\.|\\-){2,}")){
								if(!line.matches("^[\\.\\-]|[\\.\\-]$")){
									if(line.matches("^[a-zA-Z0-9\\-\\.]{2,32}\\.[a-zA-Z]{2,9}$")){
										++i;
										line=line.toLowerCase();
										if(line.startsWith("www.")) line=line.substring(4);
										SQLite.ins("host",new String[]{"domain",line,"valid","5","status","0"});
										if(i%300==0){
											int added=i;
											runOnUiThread(()->{
												TextView txtv=new TextView(getApplicationContext());
												txtv.setSingleLine(true);
												txtv.setText(added+getString(R.string.run_one_log_added));
												((LinearLayout)findViewById(R.id.logger)).addView(txtv);
												((ScrollView)findViewById(R.id.logger_parent)).fullScroll(ScrollView.FOCUS_DOWN);
											});
										}
									}
								}
							}
						}
					}
					int added=i;
					db_count+=added;
					runOnUiThread(()->{
						inp1.setText(Integer.toString(db_count));
						TextView txtv=new TextView(getApplicationContext());
						txtv.setText(getString(R.string.run_one_log_updated_now1)+db_count+getString(R.string.run_one_log_updated_now2));
						((LinearLayout)findViewById(R.id.logger)).addView(txtv);
						((ScrollView)findViewById(R.id.logger_parent)).fullScroll(ScrollView.FOCUS_DOWN);
						two_paste_text.setText("");
						if(added>0){
							findViewById(R.id.two_layout).setVisibility(View.INVISIBLE);
						}else{
							toast_show(R.string.run_one_toast_nodomainfound);
						}
					});
				}else{
					alert_box(getString(R.string.run_two_alert_addlistonly));
				}
			}
		});

	}
	@Override protected void onPause(){
		super.onPause();
		Log.e("__A","onPause");
	}
	@Override protected void onStop(){
		super.onStop();
		Log.e("__A","onStop");
	}
	@Override public void onDestroy(){
		super.onDestroy();
		Log.e("__A","onDestroy");
		if(db1!=null&&mem("switch").equals("0")){
			Log.e("__L","onDestroy: Try Close DB");
			try{
				db1.close();
				db1=null;
			}catch(Exception ignored){}
		}
	}



	public String mem(String key){
		String sw="";
		try{sw=SQLite.mem.get(key);}catch(Exception ignored){}
		return sw!=null&&sw.length()>0?sw:"";
	}
	public String mem(String key,String val){
		if(key.length()>0){
			Log.e("__E","one > SET mem["+key+"]="+val);
			try{SQLite.mem.put(key,val);}catch(Exception ignored){}
		}return val;
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

	public boolean isIgnoreBatteryOptimize(){
		boolean res;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
			try{
				PowerManager pm=(PowerManager)getApplicationContext().getSystemService(POWER_SERVICE);
				res=pm.isIgnoringBatteryOptimizations(app_pack_name);
			}catch(Exception e){
				res=false;
			}
		}else{
			res=true;
		}
		return res;
	}

	public void reqIgnoreBatteryOptimize(){
		AlertDialog.Builder alert2=new AlertDialog.Builder(this);
		alert2.setTitle(R.string.run_one_reqignoreoptimize1);
		alert2.setMessage(R.string.run_one_reqignoreoptimize2);
		alert2.setPositiveButton(R.string.run_one_reqignoreoptimize_y,new DialogInterface.OnClickListener(){
			@android.annotation.SuppressLint("BatteryLife")
			@Override public void onClick(DialogInterface dialog,int which){
				dialog.dismiss();
				try{
					Intent int2=new Intent();
					int2.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
					int2.setData(Uri.parse("package:"+app_pack_name));
					startActivity(int2);
				}catch(Exception e){
					toast_show(R.string.run_one_toast_nooptimization);
				}
			}
		});
		alert2.setNegativeButton(R.string.run_one_reqignoreoptimize_n,new DialogInterface.OnClickListener(){
			@Override public void onClick(DialogInterface dialog,int which){
				dialog.dismiss();
			}
		});
		AlertDialog dialog=alert2.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
	}

	public int get_conc(){
		EditText inp4=findViewById(R.id.inp4);
		String txt=inp4.getText().toString();
		if(txt.length()>0){
			int num=Integer.parseInt(txt);
			return Math.max(num,0);
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

	public void toast_show(int str){
		Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
	}

	public void alert_box(String str){
		new AlertDialog.Builder(this)
			.setTitle("")
			.setMessage(str)
			.setPositiveButton(android.R.string.ok,null)
			.setCancelable(false)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();
	}

	public void prompt_updatedb(){
		AlertDialog.Builder alert1=new AlertDialog.Builder(this);
		alert1.setTitle(R.string.run_one_confirm_empty1);
		alert1.setMessage(R.string.run_one_confirm_empty2);
		alert1.setCancelable(false);
		alert1.setPositiveButton(R.string.run_one_confirm_empty_auto,new DialogInterface.OnClickListener(){
			@Override public void onClick(DialogInterface dialog,int which){
				if(net_stat){
					dialog.dismiss();
					runOnUiThread(()->{
						LinearLayout ll=findViewById(R.id.logger);
						ll.removeAllViews();
						ll.invalidate();
						TextView txtv=new TextView(getApplicationContext());
						txtv.setText(R.string.run_one_log_updating);
						ll.addView(txtv);
					});
					new Thread(new Runnable(){@Override public void run(){
						updatedb(getString(R.string.update_url));
					}}).start();
				}else{
					toast_show(R.string.run_one_toast_turnon);
					finish();
				}
			}
		});
		alert1.setNegativeButton(R.string.run_one_confirm_empty_manual,new DialogInterface.OnClickListener(){
			@Override public void onClick(DialogInterface dialog,int which){
				dialog.dismiss();
				findViewById(R.id.two_layout).setVisibility(View.VISIBLE);
				EditText two_paste_text=findViewById(R.id.two_paste_text);
				two_paste_text.setHorizontallyScrolling(true);
			}
		});
		AlertDialog dialog=alert1.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
	}

	public void service(boolean turn){
		Log.e("__E","one > service SET mem[switch]="+(turn?"1":"0"));
		mem("switch",turn?"1":"0");
		SQLite.exe("update data set v='"+(turn?"1":"0")+"' where k='last_switch_stat';");
		EditText inp4=findViewById(R.id.inp4);
		EditText inp5=findViewById(R.id.inp5);
		CheckBox checkbox1=findViewById(R.id.checkbox1);
		Intent srv=new Intent(getApplication(),bgService.class);
		srv.putExtra("messenger",new Messenger(new iHandler(this)));
		inp4.setEnabled(!turn);
		inp5.setEnabled(!turn);
		checkbox1.setEnabled(!turn);
		if(turn){
			startService(srv);
		}else{
			stopService(srv);
			boolean loc_bgTile_start=false;
			try{
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
					loc_bgTile_start=bgTile.bgTile_start;
					if(isStaticVarDefined("bgTile","bgTile_start")&&loc_bgTile_start){
						bgTile.bgTile_start=false;
					}
				}
			}catch(Exception ignored){}
		}
		LinearLayout ll=findViewById(R.id.logger);
		TextView txtv=new TextView(getApplicationContext());
		txtv.setText(get_ts()+"  ------------------ "+(turn?getString(R.string.run_one_log_line_start):getString(R.string.run_one_log_line_stop)));
		ll.addView(txtv);
	}

	public void updatedb(String targetURL){
		String line;
		int i=0;
		try{
			URL url=new URL(targetURL);
			BufferedReader in=new BufferedReader(new InputStreamReader(url.openStream()));
			while((line=in.readLine())!=null){
				if(line.length()>3){
					if(!line.matches("(\\.|\\-){2,}")){
						if(!line.matches("^[\\.\\-]|[\\.\\-]$")){
							if(line.matches("^[a-zA-Z0-9\\-\\.]{2,32}\\.[a-zA-Z]{2,9}$")){
								++i;
								line=line.toLowerCase();
								if(line.startsWith("www.")) line=line.substring(4);
								SQLite.ins("host",new String[]{"domain",line,"valid","5","status","0"});
								if(i>64000){
									alert_box(getString(R.string.run_one_log_updating_break));
									break;
								}
								if(i%300==0){
									int added=i;
									runOnUiThread(()->{
										TextView txtv=new TextView(getApplicationContext());
										txtv.setSingleLine(true);
										txtv.setText(added+getString(R.string.run_one_log_added));
										((LinearLayout)findViewById(R.id.logger)).addView(txtv);
										((ScrollView)findViewById(R.id.logger_parent)).fullScroll(ScrollView.FOCUS_DOWN);
									});
								}
							}
						}
					}
				}
			}
			in.close();
			db_count=i;
		}catch(Exception ignored){}
		runOnUiThread(()->{
			LinearLayout ll=(LinearLayout)findViewById(R.id.logger);
			ll.removeAllViews();
			ll.invalidate();
			TextView txtv=new TextView(getApplicationContext());
			if(db_count>0){
				EditText inp1=findViewById(R.id.inp1);
				inp1.setText(String.valueOf(db_count));
				txtv.setText(getString(R.string.run_one_log_updated_now1)+db_count+getString(R.string.run_one_log_updated_now2));
			}else{
				txtv.setText(R.string.run_one_log_update_error);
			}
			ll.addView(txtv);
			//ScrollView sv=(ScrollView)findViewById(R.id.logger_parent);sv.post(new Runnable(){public void run(){sv.fullScroll(View.FOCUS_DOWN);}});
		});
	}

	public boolean isStaticVarDefined(String className,String varName){
		try{
			Class<?> clazz=Class.forName(app_pack_name+"."+className);
			Field field=clazz.getDeclaredField(varName);
			return Modifier.isStatic(field.getModifiers());
		}catch(Exception e){
			return false;
		}
	}
	public static class iHandler extends Handler {
		private final WeakReference<one> weakActivity;
		iHandler(one activity) {
			super(Looper.getMainLooper());
			weakActivity = new WeakReference<>(activity);
		}
		@Override public void handleMessage(@NonNull Message msg) {
			one act = weakActivity.get();
			if (act == null) return;
			HashMap hm = (HashMap)msg.obj;
			EditText inp2=act.findViewById(R.id.inp2);
			String inp2_str=inp2.getText().toString();
			int inp2_int=inp2_str.length()>0?Integer.parseInt(inp2_str):0;
			inp2.setText(String.valueOf(inp2_int+1));
			LinearLayout ll=act.findViewById(R.id.logger);
			int ll_count=ll.getChildCount();
			if(ll_count>40){
				ll.removeView(ll.getChildAt(0));
			}
			ScrollView sv=act.findViewById(R.id.logger_parent);
			String ts=new SimpleDateFormat("HH:mm:ss").format(new Date());
			String stat=hm.get("stat").toString();
			String domain=hm.get("domain").toString();
			String stat_str=stat.equals("-1")?"000 \u00A0 ×":(stat.equals("-2")?"bye \u00A0 ×":stat+" \u00A0 <");
			String log=ts+" \u00A0 > \u00A0 "+stat_str+" \u00A0 "+domain+"\n";
			TextView txtv=new TextView(weakActivity.get());
			txtv.setSingleLine(true);
			txtv.setMaxLines(1);
			txtv.setText(log);
			ll.addView(txtv);
			sv.fullScroll(ScrollView.FOCUS_DOWN);
		}
	}

}
