package fr.odai.smschat.model;

import android.content.Context;
import fr.odai.smschat.db.DBHelper;

public class POJORoom {
	
	private long id;
	public String name;
	public boolean is_bridge;
	
	public POJORoom(long id, String name, boolean is_bridge) {
		super();
		this.id = id;
		this.name = name;
		this.is_bridge = is_bridge;
	}
	
	public long getId() {
		return id;
	}
	
	public boolean isInit(Context context){
		boolean hasContacts = !DBHelper.getContactsToSend(context, id).isEmpty();
		if(hasContacts){
			return true;
		}
		return false;
	}
	
	public long getNbContacts(Context context){
		return DBHelper.getContactsToSend(context, id).size();
	}
}
