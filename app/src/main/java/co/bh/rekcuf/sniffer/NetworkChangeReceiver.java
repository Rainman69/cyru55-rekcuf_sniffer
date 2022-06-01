package co.bh.rekcuf.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.widget.Toast;
import android.widget.ToggleButton;


public class NetworkChangeReceiver extends BroadcastReceiver {

    public static ToggleButton netstat;

    static void setToggle(ToggleButton one){
        netstat=one;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        SQLite db1=new SQLite(context);// init and create tables
        boolean status = NetworkUtil.isConnected(context);
        if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            try{one.net_stat=status;}catch(Exception e){}
            db1.exe("update data set v='"+(status?"1":"0")+"' where k='last_net_stat';");
            netstat.setChecked(status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void registerNetworkCallback(Context context) {
        SQLite db2=new SQLite(netstat.getContext());// init and create tables
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerDefaultNetworkCallback(
                new ConnectivityManager.NetworkCallback(){
                    @Override
                    public void onAvailable(Network network) {
                        db2.exe("update data set v='1' where k='last_net_stat';");
                        try{
                            one.net_stat=true;
                            one.handler1.post(new Runnable(){
                                @Override
                                public void run(){
                                    netstat.setChecked(true);
                                }
                            });
                        }catch(Exception e){}
                    }
                    @Override
                    public void onLost(Network network) {
                        db2.exe("update data set v='0' where k='last_net_stat';");
                        try{
                            one.net_stat=false;
                            one.handler1.post(new Runnable(){
                                @Override
                                public void run(){
                                    netstat.setChecked(false);
                                }
                            });
                        }catch(Exception e){}
                    }
                }
            );
        }catch (Exception e){
            Toast.makeText(context,"NetworkChangeReceiver > registerNetworkCallback > Exception",Toast.LENGTH_LONG).show();
        }
    }

}
