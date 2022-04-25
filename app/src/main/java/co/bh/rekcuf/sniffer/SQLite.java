package co.bh.rekcuf.sniffer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLite extends SQLiteOpenHelper{

	public static SQLiteDatabase db1;
	public static String tablename="host";

	public SQLite(Context cx){
		super(cx,"host.db",null,1);
		db1=this.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("create table if not exists "+tablename+" (id integer primary key autoincrement,host text unique);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
		db.execSQL("drop table if exists "+tablename+";");
		onCreate(db);
	}

	public static void exe(String query){
		db1.execSQL(query);
	}
	public static boolean ins(String line){
		ContentValues cv=new ContentValues();
		cv.put("host",line);
		long res=db1.insert(tablename,null,cv);
		return res!=-1;
	}
	public static boolean del(String id){
		int res=db1.delete(tablename,"id=?",new String[]{id});
		return res>0;
	}
	public static boolean upd(String id,String str){
		ContentValues cv=new ContentValues();
		cv.put("id",id);
		cv.put("name",str);
		int res=db1.update(tablename,cv,"id=?",new String[]{id});
		return res>0;
	}
	public static Cursor sel(String query){
		Cursor res=db1.rawQuery(query,null);
		return res;
	}

}
