package co.bh.rekcuf.sniffer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLite extends SQLiteOpenHelper{

	public static SQLiteDatabase db1;

	public SQLite(Context cx){
		super(cx,"host.db",null,1);
		db1=this.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("create table if not exists host (domain text unique);");
		db.execSQL("create table if not exists data (k text unique,v text);");
		db.execSQL("create index i_k on data(k);");
		db.execSQL("insert into data(k,v) values('sent_total',0);");
		db.execSQL("insert into data(k,v) values('last_conc',4);");
		db.execSQL("insert into data(k,v) values('last_timeout',5000);");
		db.execSQL("insert into data(k,v) values('last_notif',1);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
		db.execSQL("drop table if exists host;");
		db.execSQL("drop table if exists counter;");
		onCreate(db);
	}

	public static void exe(String query){
		db1.execSQL(query);
	}
	public static boolean ins(String table,String[] arr){
		if(arr.length%2==0){
			ContentValues cv=new ContentValues();
			for(int i=0;i<arr.length;i++){
				cv.put(arr[i],arr[++i]);
			}
			long res=db1.insert(table,null,cv);
			return res!=-1;
		}
		return true;
	}
	public static Cursor sel(String query){
		Cursor res=db1.rawQuery(query,null);
		return res;
	}
	public static String se1(String query){
		Cursor res=db1.rawQuery(query,null);
		if(res.getCount()>0){
			if(res.moveToNext()){
				return res.getString(0);
			}
		}
		return "";
	}

}
