package fr.odai.smschat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import fr.odai.smschat.model.POJOMessage;
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
					if(body.startsWith("#")){
						processRoom(context, sender, content[0].substring(1), content[1], time);
					}else if(body.startsWith("/")){
						processCommand(context, sender, content[0].substring(1), content[1], time);
					}
				}
			}
		}
	}
	
	private void processCommand(Context context, String sender, String command, String params, long time){
		if(command.equalsIgnoreCase("/bridge")){
			if(params.startsWith("#")){
				String[] splited = params.split(" ", 3);
				String roomName = splited[0].substring(1);
				POJORoom room = DBHelper.getRoom(context, roomName);
				String routedSender = splited[1].split(":")[0];
				String newNick = splited[1].split(":")[1];
				DBHelper.ChangeContactNick(context, room.getId(), routedSender, newNick);
				processRoom(context, routedSender, roomName, splited[2], time);
			}
		}
	}
	
	private void processRoom(Context context, String sender, String smsRoomName, String body, long time){
		boolean found = false;
		ArrayList<POJORoom> rooms = DBHelper
				.getRooms(context);
		Iterator<POJORoom> it = rooms.iterator();
		while (it.hasNext() && !found) {
			POJORoom room = it.next();
			String nick = DBHelper.getContactNick(context, sender, room.getId());
			
			if(smsRoomName.equalsIgnoreCase(room.name)){
				found = true;
				DBHelper.insertMessage(context, room.getId(), sender, body, time);
				if(room.is_bridge){
					for (POJOContact contact : DBHelper.getContactsToSend(context, room.getId())) {
						if (!PhoneNumberUtils.compare(sender, contact.number)) {
							SmsManager smsManager = SmsManager.getDefault();
							String newBody = "";
							if(contact.is_bridge){
								newBody = "/bridge " + "#" + room.name + " " + sender + ":" + nick + " " + body;
							}else {
								newBody = "#" + room.name + "\n";
								if(!contact.use_app && (nick != null)){
									newBody += nick + " : ";
								}else {
									newBody += contact.number + " : ";
								}
								newBody += body;
							}
							ArrayList<String> parts = smsManager.divideMessage(newBody);
							smsManager.sendMultipartTextMessage(contact.number, null, parts, null, null);
						}
					}
				}
			}
		}
	}

	private static Map<String, Pair<Long, String>> RetrieveMessages(Intent intent) {
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
								new Pair<Long, String>(msgs[i].getTimestampMillis(), msgs[i].getMessageBody()));

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