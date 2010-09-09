package org.isophonics.scanvox;

import java.util.Hashtable;
import java.util.LinkedList;

import android.util.Log;

import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.SCAudio;

/**
 * Contains the gubbins to allow clients to listen out for
 * supercollider messages they might be interested in
 * 
 * @author alex
 *
 */
public class SCMessageManager {
	
	/**
	 * Implement this interface to receive messages from supercollider
	 * 
	 * @author alex
	 *
	 */
	public interface OscListener {
		public void receive(OscMessage msgFromServer);
	}

	/** How long to wait before checking supercollider again */
	public static final long POLL_TIME = 10;

	public static final String TAG = "MessageManager";
	
	private Hashtable<String,LinkedList<OscListener>> listeners
			= new Hashtable<String,LinkedList<OscListener>>();
	
	/**
	 * Register a listener to receive OSC messages
	 * 
	 * @param ol An OscListener
	 * @param messageType
	 */
	public void register(OscListener ol, String messageType) {
		LinkedList<OscListener> handlerQueue = getQueue(messageType);
		handlerQueue.add(ol);
	}
	
	/**
	 * Unregister a listener that is no longer doing anything useful.
	 * 
	 * @param ol An OscListener
	 * @param messageType
	 */
	public void unregister(OscListener ol, String messageType) {
		LinkedList<OscListener> handlerQueue = getQueue(messageType);
		handlerQueue.remove(ol);
		if (handlerQueue.isEmpty()) 
			listeners.remove(messageType);
	}
	
	private LinkedList<OscListener> getQueue(String messageType) {
		LinkedList<OscListener> handlerQueue;
		if (!listeners.containsKey(messageType)) {
			handlerQueue = new LinkedList<OscListener>();
			listeners.put(messageType, handlerQueue);
		} else {
			handlerQueue = listeners.get(messageType);
		}
		return handlerQueue;
	}
	
	public void startListening(SCAudio superCollider) {
		listening = true;
		eventLoop = new EventLoop();
		eventLoop.start();
	}
	
	private EventLoop eventLoop;
	private boolean listening;
	public void stopListening() {
		listening = false;
	}
	
	/**
	 * Start spreading the news from SuperCollider to listeners
	 * @param om
	 */
	private void notifyListeners(OscMessage om) {
		String messageType = om.get(0).toString();
		if (listeners.containsKey(messageType)) {
			LinkedList<OscListener> handlerQueue = listeners.get(messageType);
			for (OscListener ol : handlerQueue) 
				try {
					ol.receive(om);
				} catch (Exception e) {
					Log.e(TAG,"An exception happened in an oscListener for "+om.get(0).toString());
					e.printStackTrace();
				}
		}
	}

	/**
	 * This class just sits there and listens for new messages
	 * 
	 * @author alex shaw
	 *
	 */
	private class EventLoop extends Thread {
		
		public void run() {
			while (listening) {
				while (SCAudio.hasMessages()) {
					OscMessage receivedMessage = SCAudio.getMessage();
					notifyListeners (receivedMessage);
				}
				try {
					Thread.sleep(POLL_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
