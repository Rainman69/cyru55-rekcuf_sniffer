package co.bh.rekcuf.sniffer;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;


@RequiresApi(api=Build.VERSION_CODES.N)
public class bgTile extends TileService{

	@Override
	public void onTileAdded() {
		Tile tile = getQsTile();
		tile.setState(Tile.STATE_INACTIVE);
		tile.updateTile();
	}

	@Override
	public void onClick() {
		super.onClick();
		Tile tile = getQsTile();
		tileSrv(tile.getState() == Tile.STATE_INACTIVE);
	}

	public void tileSrv(boolean turn){
		one.switch_stat=turn;
		Tile tile = getQsTile();
		tile.setState(turn?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
		tile.updateTile();
		Intent srv=new Intent(bgTile.this,bgService.class);
		if(turn){
			startService(srv);
		}else{
			stopService(srv);
		}
	}

}
