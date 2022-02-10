package co.bh.rekcuf.sniffer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.util.Timer;
import java.util.TimerTask;


public class one extends AppCompatActivity {

    public Handler handler1 = new Handler();

    public String[] urls={"https://ana.ir/","http://www.ipna.ir/","http://www.irna.ir/","http://www.isna.ir/","http://www.iscanews.ir/","http://www.ilna.ir/","http://www.iqna.ir/","http://www.tabnak.ir/","http://www.tasnimnews.com/","http://www.farsnews.ir/","http://www.shana.ir/","http://www.mehrnews.com/","https://www.abna24.com/","http://www.khabaronline.ir/","http://www.bornanews.ir/","http://alef.ir/","http://entekhab.ir/","http://www.eghtesadonline.com/","http://jahannews.com/","http://sahamnews.org/","http://www.asriran.com/","http://www.fararu.com/","http://www.aftabnews.ir/","http://www.kaleme.com/","http://www.mashreghnews.ir/","http://www.majzooban.org/","https://nournews.ir/Fa/"};
    public int g_num1;

    public int get_conc(){
        EditText num1 = findViewById(R.id.num1);
        String txt = num1.getText().toString();
        if(txt.length()>0) {
            Integer num = Integer.parseInt(txt);
            if(num>0){
                g_num1=num;
            }
        }
        g_num1=0;
        return g_num1;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one);

        ToggleButton netstat = findViewById(R.id.netstat);
        ToggleButton togglev1 = findViewById(R.id.togglev1);
        EditText num1 = findViewById(R.id.num1);
        Switch switch1 = findViewById(R.id.switch1);
        Button button1 = findViewById(R.id.button1);
        //Toast.makeText(one.this,"startService",Toast.LENGTH_SHORT).show();

        NetworkChangeReceiver.setToggle(netstat);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            registerNetworkCallback(this);
        boolean stat = NetworkUtil.isConnected(getApplicationContext());
        netstat.setChecked(stat);
        //netstat.isChecked()

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Boolean stat = isServiceRunning(bgService.class);
                handler1.post(new Runnable() {
                    @Override
                    public void run() {
                        togglev1.setChecked(stat);
                    }
                });
            }
        },0,500);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean stat = isServiceRunning(bgService.class);
                String x = Boolean.toString(stat);
                Toast.makeText(one.this,"stat: "+x,Toast.LENGTH_SHORT).show();
            }
        });

        num1.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    int num = get_conc();
                    if(num>0&&num<17){
                        //num1.setBackgroundColor(Color.TRANSPARENT);
                        num1.getBackground().setColorFilter(getResources().getColor(R.color.teal_200), PorterDuff.Mode.SRC_IN);
                    }else{
                        //num1.setBackgroundColor(Color.parseColor("#FFAAAA"));
                        num1.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    }
                }
                return false;
            }
        });

        switch1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent srv = new Intent(getApplication(),bgService.class);
                if(switch1.isChecked()) {
                    num1.setEnabled(false);
                    startService(srv);
                    startThread(view);
                }else{
                    num1.setEnabled(true);
                    stopService(srv);
                }
            }
        });
        
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void registerNetworkCallback(Context ac){
        NetworkChangeReceiver.registerNetworkCallback(ac);
    }

    public void startThread(View view){
        bgThread thread1 = new bgThread(1);
        thread1.start();
    }

    public void stopThread(View view){}

    class bgThread extends Thread {
        int conc=4;
        bgThread(int conc){
            this.conc=conc;
        }
        @Override
        public void run(){
            //Toast.makeText(getApplicationContext(),"onStartCommand",Toast.LENGTH_SHORT).show();
            handler1.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(one.this,"bgThread.run()",Toast.LENGTH_SHORT).show();
                }
            });
            if(urls.length>0) {
                int x = (int)(Math.random() * urls.length);
                String url = urls[x];
                handler1.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(one.this,url,Toast.LENGTH_SHORT).show();
                    }
                });
                if(url.length()>0){}else{}
            }else{}
        }
    }

}
