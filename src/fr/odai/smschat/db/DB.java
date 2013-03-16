package fr.odai.smschat.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

	final static int DB_VERSION = 1;
	final static String DB_NAME = "smschat.s3db";
	Context context;

	public DB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL("CREATE TABLE rooms ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, is_bridge INTEGER NOT NULL)");
		database.execSQL("CREATE TABLE contacts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT NOT NULL, nick TEXT, is_bridge INTEGER NOT NULL, use_app INTEGER NOT NULL, parent_bridge TEXT, room_id INTEGER NOT NULL, FOREIGN KEY(room_id) REFERENCES rooms(_id))");
		database.execSQL("CREATE TABLE messages ( _id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, body TEXT NOT NULL, date INTEGER NOT NULL, room_id INTEGER NOT NULL, FOREIGN KEY(room_id) REFERENCES rooms(_id))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// See
		// http://www.drdobbs.com/database/using-sqlite-on-android/232900584?pgno=2
	}
}