package org.isophonics.scanvox;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import android.os.Environment;
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
	public static final long bufferSize = SCAudio.sampleRateInHz*2; // @TODO: magic number
	private static final String TAG = "SoundManager";
	private static final int addToHead = 0;
	private static final int addToTail = 1;
	private static final int addBefore = 2;
	private static final int addAfter  = 3;
	private static final int addReplace= 4;
    protected SCAudio superCollider;
    protected Vector<Integer> bufferIDs = new Vector<Integer>();
    protected int lastBufferId = -1; // NB: !== bufferIDs.size()-1, due to possible deletions
    protected static final int clockNode = 1990;
    protected static final int playersGroupNode = 1995;
    protected static final int beatBus = 0;
    protected int lastBusId = 0;
    private static int bufferChannelsDefault = 7; 
    private int treeBufId = -1;
    private int trevBufId = -1;
    protected boolean recording = false;
    private static Hashtable<String,Integer> treeBufferIds = new Hashtable<String,Integer>();
    
    public SoundManager(SCAudio s) {
    	superCollider = s;
    	initialise();
    }
    
    /**
     * Finds or loads the requested buffer of tree data.  
     */
    private int treeBuffer(String filename) throws IOException {
    	if (treeBufferIds.contains(filename)) {
    		return treeBufferIds.get(filename);
    	}
    	else {
    		File treesDir = new File(Environment.getExternalStorageDirectory(),ScanVox.scanvoxTreeDirectory);
    		File pathToTree = new File(treesDir,filename);
        	if(!sendOscAndWaitForDone(new OscMessage (new Object[] {
        			"b_allocRead",++lastBufferId,pathToTree
        	}))) {
        		Log.e(TAG,"Could not load rtree buffer");
        		throw new IOException(String.format("Could not fetch tree data file '%s'",filename));
        	}
        	treeBufferIds.put(filename, lastBufferId);
        	return lastBufferId;
    	}
    }
    
    private void initialise() {
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"s_new","clockodile", clockNode, addToHead, 1, "out", beatBus}));
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"g_new", playersGroupNode, addToTail, 1}));
    }

    /**
     * Create a buffer of the requested size, and fill it with audio from the mic.
     * Add it to the loop as soon as it's done.
     * 
     * @param size in ms
     * @param sac notify that the sound has been added
     * @param synthType 
     * @return the id of the requested buffer.
     */
    public int recordNew(long size, SoundAddedCallback sac, MappedSynth synthType) {
    	if (recording) return -1;  // @TODO: There's better ways to do mutexes
    	recording = true;
    	
    	int bufferChannels  = bufferChannelsDefault;  //
    	OscMessage bufferAllocMsg = new OscMessage( new Object[] {
    		"b_alloc",++lastBufferId,(int) ((size*SCAudio.sampleRateInHz/1000)/512),bufferChannels
    	});
    	Log.d(TAG,bufferAllocMsg.toString());
    	
    	if (!sendOscAndWaitForDone( bufferAllocMsg )) {
    		recording = false;
    		return -1;
    	}
    	OscMessage recordMsg = new OscMessage( new Object[] {
    	    "s_new","_scanvox_rec",recordNodeForId(lastBufferId),addToHead,1,"timbrebuf",lastBufferId
    	});
    	Log.d(TAG,recordMsg.toString());
    	superCollider.sendMessage( recordMsg );
    	addWhenReady(lastBufferId,size, sac, bufferChannels, synthType);
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
    private void addWhenReady(
    		final int bufferId, 
    		final long sleepTime, 
    		final SoundAddedCallback sac,
    		final int bufferChannels,
    		final MappedSynth synthType) {
		recordWait = new Thread(new Runnable(){
			public void run() {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {} 
				loopNew(bufferId,sac,bufferChannels,synthType);
			}
		});
		recordWait.start();
	}

    private Thread recordWait = null;
    
    /**
     * add a buffer to the loop
     * 
     * @param bufferId
     */
	private void loopNew(
			int bufferId, 
			SoundAddedCallback sac,
			int bufferChannels,
			MappedSynth synthType) {
		if (!recording) return;
		try {
			String playController;
			switch(synthType.getNumControls()) {
			case 7:
				playController = "_scanvox_playcontrols7";
				break;
			case 9:
				playController = "_scanvox_playcontrols9";
				break;
			default:
				Log.e(TAG,String.format("UNIMPLEMENTED BUS WIDTH FOR '%s'",synthType.getLabel()));
				recording = false;
				return;
			}
			OscMessage playMsg = new OscMessage( new Object[] {
	    	    "s_new",playController,playNodeForId(lastBufferId), addToHead, playersGroupNode,
	    	    "timbrebuf"    ,lastBufferId,
	    	    "controlsbus",lastBusId,
	    	    "paramShouldBePitch",synthType.getParamShouldBePitch(),
	    	    "treebuf",treeBuffer(synthType.getTreeFileName()),
	    	    "trevbuf",treeBuffer(synthType.getTrevmapFileName())
	    	});
			OscMessage beatMap = new OscMessage( new Object[] {
				"n_map",playNodeForId(lastBufferId),"clockbus",beatBus
			});
			OscMessage synthMessage = new OscMessage( new Object[] {
				"s_new","default",synthNodeForId(lastBufferId), addToTail, playersGroupNode
			});
			OscMessage controlMap = new OscMessage( new Object[] {
				"n_mapn",synthNodeForId(lastBufferId),2,lastBusId,synthType.getNumControls()
			});
			lastBusId += synthType.getNumControls();
			Log.d(TAG,playMsg.toString());
	    	superCollider.sendMessage( playMsg );
	    	superCollider.sendMessage( beatMap );
	    	superCollider.sendMessage( synthMessage );
	    	superCollider.sendMessage( controlMap );
	    	sac.whenSoundAdded ( lastBufferId );
		} catch (IOException e) {
			Log.e(TAG, String.format("Error loading tree buffer '%s'",e.getMessage()));
		} finally {
			recording = false;
		}
	}
	
	/**
	 * There's a standard layout for synthdef node numbers based on their
	 * id.  They are recorded in the following functions:
	 * @return
	 */
	private int recordNodeForId(int id) {return OscMessage.defaultNodeId + 3*id;}
	private int playNodeForId(int id) {return OscMessage.defaultNodeId + 3*id+1;}
	private int synthNodeForId(int id) {return OscMessage.defaultNodeId + 3*id+2;}
	
	/**
	 * Lightweight interface to asynchronously let the caller know that
	 * their sound was added successfully and gives them the ID of the 
	 * new sound.
	 * 
	 * @author Alex Shaw
	 *
	 */
	public interface SoundAddedCallback {
		public void whenSoundAdded(int id);
	}

	public void removeSound(int id) {
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",synthNodeForId(id)
		}));
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",playNodeForId(id)
		}));
	}

	/**
	 * Choose which tick to start the sound on
	 * 
	 * @param id the sound to modify
	 * @param f the tick to use
	 */
	public void setSoundStart(int id, int f) {
		OscMessage startMessage = new OscMessage(new Object[] {
				"n_set",id,"startTick",f
		});
		Log.d(TAG,startMessage.toString());
		superCollider.sendMessage(startMessage);
	}

	/**
	 * Stops the currently created recording, if any.
	 * 
	 * Assumes that there is a recordbuffer related to lastBufferId
	 */
	public void stopRecording() {
		if (!recording) return;
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",recordNodeForId(lastBufferId)
		}));
		if (recordWait != null && recordWait.isAlive()) recordWait.interrupt();
	}
	
	private boolean sendOscAndWaitForDone(OscMessage msg) {
		while (SCAudio.hasMessages()) SCAudio.getMessage(); // clean out mailbox
    	superCollider.sendMessage( msg );

    	// Wait on a positive response from SCAudio
    	OscMessage msgFromServer=null;
    	int triesToFail = 10000;
		while (msgFromServer==null && --triesToFail>0) {
    		if (SCAudio.hasMessages()) msgFromServer = SCAudio.getMessage();
    		try {
    			Thread.sleep(5);
    		} catch (InterruptedException e) {
    			break;
    		}
		}
		if (msgFromServer==null) {
			Log.e(TAG,"Did not get a confirmation message back from the server.");
			Log.d(TAG,msg.toString());
			return false;	
		}
		String firstToken = msgFromServer.get(0).toString();
		if (!firstToken.equals("/done")) {
			Log.e(TAG,"Got an unexpected response back from the server.");
			Log.e(TAG,msgFromServer.toString());
			Log.d(TAG,msg.toString());
			return false;
		}

		return true;
		
	}
}
