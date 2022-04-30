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
        boolean status = NetworkUtil.isConnected(context);
        if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            one.stat=status;
            netstat.setChecked(status);
            String text="Internet "+(status?"Connected":"Disconnected");
            Toast.makeText(context,text,Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void registerNetworkCallback(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerDefaultNetworkCallback(
                new ConnectivityManager.NetworkCallback(){
                    @Override
                    public void onAvailable(Network network) {
                        one.stat=true;
                        netstat.setChecked(true);
                    }
                    @Override
                    public void onLost(Network network) {
                        one.stat=false;
                        netstat.setChecked(false);
                    }
                }
            );
        }catch (Exception e){
            Toast.makeText(context,"Err1",Toast.LENGTH_LONG).show();
        }
    }

}
