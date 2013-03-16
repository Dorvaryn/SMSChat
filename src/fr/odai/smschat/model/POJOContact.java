package fr.odai.smschat.model;

public class POJOContact {
	
	public String number;
	public String nick;
	public String parentBridge;
	public boolean use_app;
	public boolean is_bridge;
	
	
	public POJOContact(String number, String nick, String parentBridge,
			boolean use_app, boolean is_bridge) {
		super();
		this.number = number;
		this.nick = nick;
		this.parentBridge = parentBridge;
		this.use_app = use_app;
		this.is_bridge = is_bridge;
	}
}
