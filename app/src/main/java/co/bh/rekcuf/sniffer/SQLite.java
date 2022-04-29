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
		db.execSQL("create table if not exists log (ts integer,stat integer,domain text);");
		db.execSQL("create table if not exists data (k text unique,v text);");
		db.execSQL("create index i_k on data(k);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
		db.execSQL("drop table if exists host;");
		db.execSQL("drop table if exists log;");
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
	public static boolean del(String id){
		int res=db1.delete("host","id=?",new String[]{id});
		return res>0;
	}
	public static boolean upd(String id,String str){
		ContentValues cv=new ContentValues();
		cv.put("id",id);
		cv.put("name",str);
		int res=db1.update("host",cv,"id=?",new String[]{id});
		return res>0;
	}
	public static Cursor sel(String query){
		Cursor res=db1.rawQuery(query,null);
		return res;
	}

}
