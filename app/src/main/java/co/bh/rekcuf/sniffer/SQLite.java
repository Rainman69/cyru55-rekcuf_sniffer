package co.bh.rekcuf.sniffer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;


public class SQLite extends SQLiteOpenHelper{

	public static SQLiteDatabase db1;

	public SQLite(Context cx){
		super(cx,"host.db",null,1);
		db1=this.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("create table if not exists host (domain text unique,valid integer,status integer);");
		db.execSQL("create table if not exists data (k text unique,v text);");
		db.execSQL("create index i_k on data(k);");
		db.execSQL("insert into data(k,v) values('sent_total',0);");
		db.execSQL("insert into data(k,v) values('last_conc',2);");
		db.execSQL("insert into data(k,v) values('last_timeout',5000);");
		db.execSQL("insert into data(k,v) values('last_notif',1);");
		db.execSQL("insert into data(k,v) values('last_switch_stat',0);");
		db.execSQL("insert into data(k,v) values('last_net_stat',0);");
		db.execSQL("insert into data(k,v) values('ask_ignore_battery',5);");
		db.execSQL("insert into data(k,v) values('tile_killed_bg',0);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
		db.execSQL("drop table if exists host;");
		db.execSQL("drop table if exists data;");
		onCreate(db);
	}

	public static void exe(String query){
		int retry=6;
		boolean executed=false;
		while(!executed && --retry>0){
			try{
				db1.execSQL(query);
				executed=true;
			}catch(Exception e){// SQLiteDatabase.SQLITE_BUSY || SQLiteDatabase.SQLITE_LOCKED
				try{
					Thread.sleep(200);
				}catch(InterruptedException ex){
					Thread.currentThread().interrupt();
				}
			}
		}
	}
	public static boolean ins(String table,String[] arr){
		if(arr.length%2==0){
			ContentValues cv=new ContentValues();
			for(int i=0;i<arr.length;i++){
				cv.put(arr[i],arr[++i]);
			}
			long res=-2;
			int retry=6;
			boolean executed=false;
			while(!executed && --retry>0){
				try{
					res=db1.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
					executed=true;
				}catch(Exception e){
					try{
						Thread.sleep(200);
					}catch(InterruptedException ex){
						Thread.currentThread().interrupt();
					}
				}
			}
			return res!=-1;
		}
		return false;
	}
	public static Cursor sel(String query){
		Cursor res=null;
		try{
			res=db1.rawQuery(query,null);
		}catch(Exception e){
			e.printStackTrace();
		}
		return res;
	}
	public static String se1(String query){
		String out="";
		Cursor res=sel(query);
		if(res!=null){
			int count=0;
			try{
				count=res.getCount();
			}catch(Exception ignored){}
			if(count>0){
				if(res.moveToNext()){
					try{
						out=res.getString(0);
					}catch(Exception ignored){}
				}
			}
			res.close();
		}
		return out;
	}
	public static HashMap<String,String> se1row(String query){
		HashMap<String,String> kv=new HashMap<>();
		Cursor res=sel(query);
		if(res!=null){
			int count=0;
			try{
				count=res.getCount();
			}catch(Exception ignored){}
			if(count>0){
				res.moveToFirst();
				int rows=(int)res.getColumnCount();
				for(int i=0; i<rows; i++){
					String k="",v="-Exception-";
					try{
						k=res.getColumnName(i);
						v=res.getString(i);
					}catch(Exception ignored){}
					if(k.length()>0){
						kv.put(k,v);
					}
				}
				res.close();
			}
		}
		return kv;
	}

}
