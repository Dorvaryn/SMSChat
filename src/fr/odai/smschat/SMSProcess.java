package fr.odai.smschat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Pair;
import fr.odai.smschat.db.DBHelper;
import fr.odai.smschat.model.POJOContact;
import fr.odai.smschat.model.POJORoom;

public class SMSProcess extends BroadcastReceiver {

	public static final String SMS_EXTRA_NAME = "pdus";
	private final String ACTION_RECEIVE_SMS = "android.provider.Telephony.SMS_RECEIVED";

	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals(ACTION_RECEIVE_SMS)) {
			// Get the SMS map from Intent
			Map<String, Pair<Long, String>> msg = RetrieveMessages(intent);
			if (msg != null) {
				for (String sender : msg.keySet()) {
					String body = msg.get(sender).second;
					long time = msg.get(sender).first;

					String[] content = body.split(" ", 2);
					if (body.startsWith("#")) {
						processRoom(context, sender, content[0].substring(1),
								content[1], time);
					} else if (body.startsWith("/")) {
						processCommand(context, sender,
								content[0].substring(1), content[1], time);
					}
				}
			}
		}
	}

	private void processCommand(Context context, String sender, String command,
			String params, long time) {
		if (command.equalsIgnoreCase("bridge")) {
			if (params.startsWith("#")) {
				String[] splited = params.split(" ", 3);
				String roomName = splited[0].substring(1);
				POJORoom room = DBHelper.getRoom(context, roomName);
				if(room != null){
					String routedSender = splited[1].split(":")[0];
					String newNick = splited[1].split(":")[1];
					DBHelper.ChangeContactNick(context, room.getId(), routedSender,
							newNick);
					processRoom(context, routedSender, roomName, splited[2], time);
				}
			} else if (params.startsWith("register")) {
				String[] splited = params.split(" ", 2);
				if (splited[0].startsWith("#")) {
					String[] contacts = splited[1].split(";");
					processRegister(context, sender, splited[0].substring(1), contacts);
				}
			} else if(params.startsWith("update")){
				String[] splited = params.split(" ", 2);
				if (splited[0].startsWith("#")) {
					String[] contacts = splited[1].split(";");
					processUpdate(context, sender, splited[0].substring(1), contacts);
				}
			}
		}else if(command.equalsIgnoreCase("join")){
			if (params.startsWith("#")) {
				String[] splited = params.split(" ", 2);
				String smsRoomName = splited[0].substring(1);
				processJoin(context, sender, smsRoomName, splited[1].split(" "));
			}
		}else if(command.equalsIgnoreCase("list")){
			processList(context, sender);
		}
	}
	
	private void processList(Context context, String sender){
		String roomsInfo = "!listed";
		ArrayList<POJORoom> rooms = DBHelper.getRooms(context);
		for (POJORoom room : rooms) {
			if(room.is_bridge){
				roomsInfo += " #" + room.name;
			}
		}
		
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager
				.divideMessage(roomsInfo);
		smsManager.sendMultipartTextMessage(sender,
				null, parts, null, null);
	}
	
	private void processJoin(Context context, String sender,
			String smsRoomName, String[] params) {
		
		POJORoom room = DBHelper.getRoom(context, smsRoomName);
		if(room != null){
			POJOContact existing = DBHelper.getContact(context, room.getId(), sender);
			if(existing != null){
				//TODO notify user he is registered on another node
			}else {
				String nick = null;
				boolean use_app = false;
				if((params.length >= 3) && params[1].equalsIgnoreCase("as")){
					nick = params[2];
					if(params.length >= 4){
						use_app = params[3].equalsIgnoreCase("true");
					}
				}else if(params.length == 2){
					use_app = params[1].equalsIgnoreCase("true");
				}
				DBHelper.insertContact(context, room.getId(), sender, nick, null, use_app, false);
				String nickSent = "";
				if(nick != null){
					nickSent = nick;
				}
				String infos = sender + ":" + "" + ":" + nickSent + ":" + Boolean.toString(use_app);
				Iterator<POJOContact> it = DBHelper.getBridges(context,
						room.getId()).iterator();
				while (it.hasNext()) {
					POJOContact bridge = (POJOContact) it.next();
					SmsManager smsManager = SmsManager.getDefault();
					String newBody = "/bridge update #" + room.name
							+ " " + infos;
					ArrayList<String> parts = smsManager
							.divideMessage(newBody);
					smsManager.sendMultipartTextMessage(bridge.number,
							null, parts, null, null);
				}
				
				//TODO notify the user he entered the room
				
			}
		}
	}
	
	
	private void processUpdate(Context context, String sender,
			String smsRoomName, String[] contacts) {

		POJORoom room = DBHelper.getRoom(context, smsRoomName);
		if(room != null){
			for (int i = 0; i < contacts.length; i++) {
				String[] contactInfos = contacts[i].split(":");
				if(contactInfos.length > 3){
					POJOContact contact = DBHelper.getContact(context,
							room.getId(), contactInfos[0]);
					if (contactInfos[1].equalsIgnoreCase("bridge")) {
						if (contact != null) {
							contact.parentBridge = null;
							contact.use_app = true;
							contact.is_bridge = true;
							DBHelper.updateContact(context, room.getId(), contact);
						} else {
							String nick = null;
							if(!contactInfos[2].equalsIgnoreCase("")){
								nick = contactInfos[2];
							}
							DBHelper.insertContact(context, room.getId(),
									contactInfos[0], nick, null,
									contactInfos[3].equalsIgnoreCase("1"), true);
						}
					} else {
						if (contact != null) {
							String parent = sender;
							if(!contactInfos[1].equalsIgnoreCase("")){
								parent = contactInfos[1];
							}
							contact.parentBridge = parent;
						} else {
							String nick = null;
							if(!contactInfos[2].equalsIgnoreCase("")){
								nick = contactInfos[2];
							}
							String parentBridge = sender;
							if(!contactInfos[1].equalsIgnoreCase("")){
								parentBridge = contactInfos[1];
							}
							DBHelper.insertContact(context, room.getId(),
									contactInfos[0], nick, parentBridge,
									contactInfos[3].equalsIgnoreCase("true"), false);
						}
					}
				}
			}
		}
	}

	private void processRegister(Context context, String sender,
			String smsRoomName, String[] contacts) {
		
		POJORoom room = DBHelper.getRoom(context, smsRoomName);
		if(room != null){
			POJOContact newBridge = DBHelper.getContact(context, room.getId(),
					sender);
			newBridge.is_bridge = true;
			newBridge.use_app = true;
			newBridge.parentBridge = null;
			DBHelper.updateContact(context, room.getId(), newBridge);
			
			processUpdate(context, sender, smsRoomName, contacts);
			
			ArrayList<POJOContact> contactsUpdated = DBHelper.getContacts(context, room.getId());
			String infos = "";
			for (POJOContact contactUpdated : contactsUpdated) {
				String parentBridge = "";
				if(!contactUpdated.is_bridge){
					parentBridge = contactUpdated.parentBridge;
				}else {
					parentBridge = "bridge";
				}
				String nick = "";
				if(contactUpdated.nick != null){
					nick = contactUpdated.nick;
				}
				infos += contactUpdated.number + ":" + parentBridge + ":" + nick + ":" + Boolean.toString(contactUpdated.use_app) +";";
			}
			
			Iterator<POJOContact> it = DBHelper.getBridges(context,
					room.getId()).iterator();
			while (it.hasNext()) {
				POJOContact bridge = (POJOContact) it.next();
				SmsManager smsManager = SmsManager.getDefault();
				String newBody = "/bridge update #" + room.name
						+ " " + infos;
				ArrayList<String> parts = smsManager
						.divideMessage(newBody);
				smsManager.sendMultipartTextMessage(bridge.number,
						null, parts, null, null);
			}
		}
	}

	private void processRoom(Context context, String sender,
			String smsRoomName, String body, long time) {
		
		POJORoom room = DBHelper.getRoom(context, smsRoomName);
		if(room != null){
			String nick = DBHelper
					.getContactNick(context, sender, room.getId());
	
			DBHelper.insertMessage(context, room.getId(), sender, body,
					time);
			if (room.is_bridge) {
				for (POJOContact contact : DBHelper.getContactsToSend(
						context, room.getId())) {
					if (!PhoneNumberUtils.compare(sender, contact.number)) {
						SmsManager smsManager = SmsManager.getDefault();
						String newBody = "";
						if (contact.is_bridge) {
							newBody = "/bridge " + "#" + room.name + " "
									+ sender + ":" + nick + " " + body;
						} else {
							newBody = "#" + room.name + "\n";
							if (!contact.use_app && (nick != null)) {
								newBody += nick + " : ";
							} else {
								newBody += contact.number + " : ";
							}
							newBody += body;
						}
						ArrayList<String> parts = smsManager
								.divideMessage(newBody);
						smsManager.sendMultipartTextMessage(contact.number,
								null, parts, null, null);
					}
				}
			}
		}
	}

	private static Map<String, Pair<Long, String>> RetrieveMessages(
			Intent intent) {
		Map<String, Pair<Long, String>> msg = null;
		SmsMessage[] msgs = null;
		Bundle bundle = intent.getExtras();

		if (bundle != null && bundle.containsKey("pdus")) {
			Object[] pdus = (Object[]) bundle.get("pdus");

			if (pdus != null) {
				int nbrOfpdus = pdus.length;
				msg = new HashMap<String, Pair<Long, String>>(nbrOfpdus);
				msgs = new SmsMessage[nbrOfpdus];

				// There can be multiple SMS from multiple senders, there can be
				// a maximum of nbrOfpdus different senders
				// However, send long SMS of same sender in one message
				for (int i = 0; i < nbrOfpdus; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

					String originatinAddress = msgs[i].getOriginatingAddress();

					// Check if index with number exists
					if (!msg.containsKey(originatinAddress)) {
						// Index with number doesn't exist
						// Save string into associative array with sender number
						// as index
						msg.put(msgs[i].getOriginatingAddress(),
								new Pair<Long, String>(msgs[i]
										.getTimestampMillis(), msgs[i]
										.getMessageBody()));

					} else {
						// Number has been there, add content but consider that
						// msg.get(originatinAddress) already contains
						// sms:sndrNbr:previousparts of SMS,
						// so just add the part of the current PDU
						String previousparts = msg.get(originatinAddress).second;
						previousparts = previousparts
								+ msgs[i].getMessageBody();
					}
				}
			}
		}

		return msg;
	}
}