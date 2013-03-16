package fr.odai.smschat.model;


public class POJOMessage {
	
	public long date;
	public String sender;
	public String body;
	
	public POJOMessage(long date, String sender, String body) {
		super();
		this.date = date;
		this.sender = sender;
		this.body = body;
	}
	
}
