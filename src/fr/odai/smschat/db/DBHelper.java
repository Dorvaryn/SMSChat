package fr.odai.smschat.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;
import fr.odai.smschat.model.DisplayContact;
import fr.odai.smschat.model.POJOContact;
import fr.odai.smschat.model.POJOMessage;
import fr.odai.smschat.model.POJORoom;

public class DBHelper {

	public static final Object sDataLock = new Object();

	private static SQLiteDatabase getDatabase(Context context) {
		DB db = new DB(context);
		return db.getReadableDatabase();
	}

	public static void insertContact(Context context, long room_id,
			String phoneNumber) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			ContentValues values = new ContentValues();
			values.put("number", phoneNumber);
			values.put("room_id", room_id);
			values.put("use_app", false);
			values.put("is_bridge", false);
			db.insert("contacts", null, values);
			db.close();
		}
	}

	public static void insertMessage(Context context, long room_id, String sender, String body, long timestamp) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			ContentValues values = new ContentValues();
			values.put("sender", sender);
			values.put("body", body);
			values.put("date", timestamp);
			values.put("room_id", room_id);
			db.insert("keywords", null, values);
			db.close();
		}
	}

	public static long insertRoom(Context context, String name, boolean is_bridge) {
		long id = 0;
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("is_bridge", is_bridge);
			id = db.insert("rooms", null, values);
			db.close();
		}
		return id;
	}

	public static ArrayList<DisplayContact> getDisplayContacts(Context context,
			long room_id) {

		ArrayList<DisplayContact> contacts = new ArrayList<DisplayContact>();
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "contacts",
					new String[] { "number" }, "room_id = ?", new String[]{String.valueOf(room_id)}, null, null,
					"_id DESC", null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				DisplayContact entry = new DisplayContact(context,
						cursor.getString(0));

				contacts.add(entry);
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return contacts;
	}
	
	public static ArrayList<POJOContact> getContactsToSend(Context context, long room_id) {

		ArrayList<POJOContact> contacts = new ArrayList<POJOContact>();
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "contacts",
					new String[] { "number, nick, use_app, is_bridge" }, "room_id = ? AND parent_bridge IS NULL", 
					new String[]{String.valueOf(room_id)}, null, null, "_id DESC", null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				boolean use_app = (cursor.getInt(2) == 1);
				boolean is_bridge = (cursor.getInt(4) == 1);
				String nick = null;
				if(!cursor.isNull(1)){
					nick = cursor.getString(1);
				}
				contacts.add(new POJOContact(cursor.getString(0), nick, null, use_app, is_bridge));
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return contacts;
	}
	
	public static String getContactNick(Context context, String number, long room_id) {
		String nick = null;
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "contacts",
					new String[] { "number, nick" }, "room_id = ?", new String[]{String.valueOf(room_id)}, null, null,
					"_id DESC", null);

			boolean found = false;
			cursor.moveToFirst();
			while (!cursor.isAfterLast() && !found) {
				if(PhoneNumberUtils.compare(number, cursor.getString(0))){
					found = true;
					if(!cursor.isNull(1)){
						nick = cursor.getString(1);
					}
				}
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return nick;
	}

	public static ArrayList<POJORoom> getRooms(Context context) {

		ArrayList<POJORoom> rooms = new ArrayList<POJORoom>();
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "rooms", new String[] {
					"_id", "name", "is_bridge"}, null, null, null, null,
					"_id DESC", null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				boolean is_bridge = cursor.getInt(2) == 1;
				POJORoom entry = new POJORoom(cursor.getLong(0),
						cursor.getString(1), is_bridge);
				rooms.add(entry);
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return rooms;
	}
	
	/*public static ArrayList<POJORoom> getLocalRooms(Context context) {

		ArrayList<POJORoom> rooms = new ArrayList<POJORoom>();
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "rooms", new String[] {
					"_id", "name", "distant"}, "distant = ?", new String[]{String.valueOf(0)}, null, null,
					"_id DESC", null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				boolean distant = cursor.getInt(2) == 1;
				POJORoom entry = new POJORoom(cursor.getLong(0),
						cursor.getString(1), distant);
				rooms.add(entry);
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return rooms;
	}*/
	
	public static POJORoom getRoom(Context context, long room_id) {
		POJORoom entry;
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "rooms", new String[] {
					"_id", "name", "is_bridge" }, "_id = ?", new String[]{String.valueOf(room_id)}, null, null,
					"_id DESC", null);
			cursor.moveToFirst();
			boolean is_bridge = cursor.getInt(2) == 1;
			entry = new POJORoom(cursor.getLong(0),
					cursor.getString(1), is_bridge);
			cursor.close();
			db.close();
		}
		return entry;
	}
	
	public static POJORoom getRoom(Context context, String name) {
		POJORoom entry = null;
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "rooms", new String[] {
					"_id", "name", "is_bridge" }, "name = ?", new String[]{name}, null, null,
					"_id DESC", null);
			if(cursor.moveToFirst()){
				boolean is_bridge = cursor.getInt(2) == 1;
				entry = new POJORoom(cursor.getLong(0),
						cursor.getString(1), is_bridge);
			}
			cursor.close();
			db.close();
		}
		return entry;
	}

	public static ArrayList<POJOMessage> getMessages(Context context, long room_id) {

		ArrayList<POJOMessage> messages = new ArrayList<POJOMessage>();
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "messages",
					new String[] { "sender, body, date" }, "room_id = ?", new String[]{String.valueOf(room_id)}, null, null,
					"_id DESC", null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				POJOMessage entry = new POJOMessage(cursor.getLong(2), cursor.getString(0), cursor.getString(1));
				messages.add(entry);
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		}
		return messages;
	}
	
	public static void updateRoom(Context context, POJORoom room){
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);
			
			ContentValues values = new ContentValues();
			values.put("name", room.name);
			values.put("is_bridge", room.is_bridge);
			db.update("rooms", values, "_id = ?", new String[]{String.valueOf(room.getId())});
			db.close();
		}
	}
	
	public static void ChangeContactNick(Context context, long room_id,
			String phoneNumber, String nick) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			Cursor cursor = db.query(true, "contacts",
					new String[] { "number" }, "room_id = ?", new String[]{String.valueOf(room_id)}, null, null,
					"_id DESC", null);

			boolean found = false;
			cursor.moveToFirst();
			String localNumber = null;
			while (!cursor.isAfterLast() && !found) {
				if(PhoneNumberUtils.compare(phoneNumber, cursor.getString(0))){
					found = true;
					if(!cursor.isNull(1)){
						localNumber = cursor.getString(0);
					}
				}
				cursor.moveToNext();
			}
			cursor.close();
			if(localNumber != null){
				ContentValues values = new ContentValues();
				values.put("nick",nick);
				db.update("contacts", values, "number = ? AND room_id = ?", new String[] { localNumber, String.valueOf(room_id) });
			}
			db.close();
		}
	}
	
	public static void SetContactParentBridge(Context context, long room_id,
			String phoneNumber, String bridge) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			ContentValues values = new ContentValues();
			values.put("parent_bridge",bridge);
			db.update("contacts", values, "number = ? AND room_id = ?", new String[] { phoneNumber, String.valueOf(room_id) });
			db.close();
		}
	}
	
	public static void SetContactLocal(Context context, long room_id,
			String phoneNumber) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			ContentValues values = new ContentValues();
			values.putNull("parent_bridge");
			db.update("contacts", values, "number = ? AND room_id = ?", new String[] { phoneNumber, String.valueOf(room_id) });
			db.close();
		}
	}

	public static void removeContact(Context context, long room_id, String phoneNumber) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);

			db.delete("contacts", "number = ? AND room_id = ?", new String[] { phoneNumber, String.valueOf(room_id) });
			db.close();
		}
	}
	
	public static void removeRoom(Context context, long room_id) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getDatabase(context);
			db.delete("rooms", "_id = ?", new String[] { String.valueOf(room_id) });
			db.delete("messages", "room_id = ?", new String[] { String.valueOf(room_id) });
			db.delete("contacts", "room_id = ?", new String[] { String.valueOf(room_id) });
			db.close();
		}
	}
}
