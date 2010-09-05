package org.isophonics.scanvox;

import java.util.Vector;

import android.util.Log;
import android.widget.Toast;

import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.SCAudio;
import net.sf.supercollider.android.ScService;

/**
 * Provides the glue between buffers in SuperCollider and sounds in the
 * arrangement. 
 * 
 * @TODO: It'd be cool if we could keep all the state inside superCollider,
 * not sure how feasible this is
 * 
 * @author alex shaw
 *
 */
public class SoundManager {
	public long bufferSize = SCAudio.sampleRateInHz*2; // @TODO: magic number
	private static final String TAG = "SoundManager";
    protected SCAudio superCollider;
    protected Vector<Integer> bufferIDs = new Vector<Integer>();
    protected int lastBufferId = -1; // NB: !== bufferIDs.size()-1, due to possible deletions
    protected static final int clockNode = 1999;
    protected static final int beatBus = 0;
    
    protected boolean recording = false;
    
    public SoundManager(SCAudio s) {
    	superCollider = s;
    	initialiseClock();
    }
    
    private void initialiseClock() {
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"s_new","clockodile", clockNode, 0, 1, "out", beatBus}));
    }

    /**
     * Create a buffer of the requested size, and fill it with audio from the mic.
     * Add it to the loop as soon as it's done.
     * 
     * @TODO: WARNING: size*SCAudio.sampleRateInHz/1000 MUST BE 0 mod 64 
     * 
     * @param size in ms
     * @param sac notify that the sound has been added
     * @return the id of the requested buffer.
     */
    public int recordNew(long size, SoundAddedCallback sac) {
    	if (recording) return -1;  // @TODO: There's better ways to do mutexes
    	recording = true;
    	OscMessage bufferAllocMsg = new OscMessage( new Object[] {
    		"b_alloc",++lastBufferId,(int) (size*SCAudio.sampleRateInHz/1000)
    	});
    	Log.d(TAG,bufferAllocMsg.toString());
    	
    	while (SCAudio.hasMessages()) SCAudio.getMessage(); // clean out mailbox
    	superCollider.sendMessage( bufferAllocMsg );

    	// Wait on a positive response from SCAudio
    	OscMessage msgFromServer=null;
    	int triesToFail = 500;
		while (msgFromServer==null && --triesToFail>0) {
    		if (SCAudio.hasMessages()) msgFromServer = SCAudio.getMessage();
    		try {
    			Thread.sleep(5);
    		} catch (InterruptedException e) {
    			break;
    		}
		}
		if (msgFromServer==null) {
			recording = false;
			return -1;
		}
		String firstToken = msgFromServer.get(0).toString();
		if (!firstToken.equals("/done")) {
			recording = false;
			return -1;
		}

    	OscMessage recordMsg = new OscMessage( new Object[] {
    	    "s_new","recordbuffer",OscMessage.defaultNodeId + 2*lastBufferId,0,1,"buffnum",lastBufferId
    	});
    	Log.d(TAG,recordMsg.toString());
    	superCollider.sendMessage( recordMsg );
    	addWhenReady(lastBufferId,size, sac);
    	return lastBufferId;
    }
    
    /**
     * Tells the looper to start playing the requested buffer as soon as
     * it is ready (recording has finished).
     * 
     * @TODO: could this be added programmatically?
     * 
     * @param bufferId
     */
    private void addWhenReady(final int bufferId, final long sleepTime, final SoundAddedCallback sac) {
		new Thread(new Runnable(){
			public void run() {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				loopNew(bufferId,sac);
			}
		}).start();
	}

    /**
     * add a buffer to the loop
     * 
     * @param bufferId
     */
	public void loopNew(int bufferId, SoundAddedCallback sac) {
		OscMessage playMsg = new OscMessage( new Object[] {
    	    "s_new","playbuffer",OscMessage.defaultNodeId + 2*lastBufferId+1,0,1,"buffnum",lastBufferId
    	});
		OscMessage beatMap = new OscMessage( new Object[] {
			"n_map","trigger",beatBus
		});
		Log.d(TAG,playMsg.toString());
    	superCollider.sendMessage( playMsg );
    	superCollider.sendMessage( beatMap );
    	sac.whenSoundAdded(lastBufferId);
    	recording = false;
    }
	
	/**
	 * Lightweight interface to asynchronously let the caller know that
	 * their sound was added successfully and gives them the ID of the 
	 * new sound.
	 * 
	 * @author alex
	 *
	 */
	public interface SoundAddedCallback {
		public void whenSoundAdded(int id);
	}

	public void removeSound(int id) {
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",OscMessage.defaultNodeId+2*id+1
		}));
	}
}
