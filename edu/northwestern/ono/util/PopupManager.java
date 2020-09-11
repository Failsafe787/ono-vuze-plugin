package edu.northwestern.ono.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;

import edu.northwestern.ono.Main;
import edu.northwestern.ono.OnoConfiguration;

/**
 * @author Mario Sanchez &lt;msanchez@eecs.northwestern.edu&gt;
 *
 * The PopupManager class ...
 */

public class PopupManager implements Runnable {

	public class Message {
		String text;
		int timeout; //seconds
		boolean repeatable;
		String id;
		boolean audible;
		String details;
		
		public Message(String id, String text, int timeout, boolean repeatable) {
			this.id = id;
			this.text = text;
			this.timeout = timeout;
			this.repeatable = repeatable;
			this.audible = false;
			this.details = new String();
		}
		
		public Message(String id, String text, int timeout, boolean repeatable, boolean audible, String details) {
			this(id, text, timeout, repeatable);
			this.audible = audible;
			this.details = details;
		}
		
		public boolean isRepeatable() {
			return repeatable;
		}
		
		public boolean isAudible() {
			return audible;
		}
		
		public String toString() {			
			return "id: " + id + " message: " + text + " timeout: " + timeout + " repeatable: " + repeatable + " audible: " + audible + " details: " + details; 
		}
		
		public boolean equals(Object obj) {
			
			if (!(obj instanceof Message))
				return false;		
			
			return ((Message) obj).id.equals(this.id);
			
		}
		
		public int hashCode() {
			return id.hashCode();
		}
		
	}
	
	private static PopupManager self = null;
	private static int schedulerInterval = 
		OnoConfiguration.getInstance().getMessageManagerInterval();
	
	private static LinkedHashMap<String, Long> messageLog =
		Main.getPersistentDataManager().getMessageLog();
	private static LinkedHashSet<Message> messages = new LinkedHashSet<Message>();
	private static HashSet<String> alreadyDisplayed = new HashSet<String>();
	
	private Boolean isActive = false;
	public Object halt = new Object();
	private boolean terminate = false;
	private static boolean DEBUG = false;

	private PopupManager() {
		super();
		isActive = true;
	}
	
    public static PopupManager getInstance() {
    	if (self == null)
    		self = new PopupManager();
	    return self;
    }
    
    
	public void addMessage (String id, String msg, int timeout, boolean repeatable, boolean audible, String details) {
		
		Message message =new Message(id, msg, timeout, repeatable, audible, details);		
		if (DEBUG)
			System.out.println("addMessage(): " + message);

		/* if message already displayed, don't add it to the queue */
		synchronized(messageLog) {
			if (!messageLog.containsKey(id)) {
				
				synchronized(alreadyDisplayed) {
					if ( !alreadyDisplayed.contains(id) ) {
						messages.add(message);
						synchronized(halt) {
							halt.notify();
						}
					}
				}
			}
		}
	}
	
	public void run() {
		
		//if (DEBUG)
		//	schedulerInterval = 5;
		
		while (isActive) {
					
			if (DEBUG)
				System.out.println("PopupManager(): I'm awake!!");
			
			checkMessages();
			
			Iterator<Message> iter = messages.iterator();
			while (iter.hasNext()) {
				
				final Message m = iter.next();
  				iter.remove();
				
  				if (OnoConfiguration.getInstance().isEnablePopups()) {
  					
  					//Logger.log(new LogAlert(m.repeatable, LogAlert.AT_INFORMATION, "Dasu Plug-in:\n" + m.text, m.timeout));
  					
  					if ( m.isAudible())
  						Utils.beep();
  					
  					Utils.execSWTThread(new Runnable(){

  						public void run() {
  							new MessageSlideShell(Utils.getDisplay(), SWT.ICON_INFORMATION, "Ono Plug-in",
  									m.text, m.details, m.timeout);  							
  						}});
  					
  				}
  				
  				if ( !m.isRepeatable() ) {
  					
  					synchronized(messageLog) {
  						messageLog.put(m.id, System.currentTimeMillis());
  					}  					
  				}

  				synchronized(alreadyDisplayed) {
  					alreadyDisplayed.add(m.id);
  				}
			}
			
			
			synchronized (halt) {

				try {				
					if (DEBUG)
						System.out.println("PopupManager(): sleeping for " + schedulerInterval + " seconds...");
					
					halt.wait(schedulerInterval*1000);
					
				} catch (InterruptedException e) {					
				}	
			}
			
			if (terminate) {

				self = null;
				return;
			}

		}//while
		
	}
	
	public void checkMessages() {

		if (DEBUG) {
			System.out.println("checkMessages(): messages.size(): " + messages);
			System.out.println("checkMessages(): messageLog.size(): " + messageLog);
		}
		
		//String url = DasuManager.getInstance().getNewRandomDasuUrl();
		//url = url + DasuConfiguration.DASU_MESSAGES;
		
		//String url = "http://mas939-imac.cs.northwestern.edu/dasu/ws/new-message-list.dat";
		String url = "http://haleakala.cs.northwestern.edu/ono/ws/message-list.php";
		
		if (DEBUG)
			System.out.println("contacting: " + url);
		
		/*load file from server*/
		Properties props = Main.downloadFromURL(url,  2*1000);

		if (props != null) {

			try {
				Iterator<Entry<Object, Object>> iter = props.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<Object, Object> ent = iter.next();
					String id = (String)ent.getKey();
					
					int timeout = -1;
					boolean repeatable = false;
					boolean audible = false;					
					String msg = (String)ent.getValue();
					String details = "";
					
					try {
						if ( msg.contains("|")) {
							String[] parts = msg.split("\\|");
							if ( parts.length >= 4) {
								msg = parts[0];
								timeout = Integer.parseInt(parts[1]);
								repeatable = parts[2].equals("true");
								audible = parts[3].endsWith("true");
								
								if ( parts.length == 5) {
									details = parts[4];
								}
							}
							
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
	
					addMessage(id, msg, timeout, repeatable, audible, details);
					
				}
				
			} catch (Exception e) {
				if (DEBUG)
					System.out.println("DEBUG: " + e.getMessage());
				
			}
		}	
		
	}
			
	
	public void stop() {
		
		if ( DEBUG ) 
			System.out.println("PopupManager: Buh-bye!");
		
		isActive = false;
		synchronized(halt) {
			terminate = true;
			halt.notify();
		}
	}
	
}
