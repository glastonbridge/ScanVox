package org.isophonics.scanvox;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

//import android.os.Environment;
import android.util.Log;

import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.SCAudio;

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
	private static final int addToHead = 0;
	private static final int addToTail = 1;
	@SuppressWarnings("unused")
	private static final int addBefore = 2;
	private static final int addAfter  = 3;
	@SuppressWarnings("unused")
	private static final int addReplace= 4;
    protected SCAudio superCollider;
    protected Vector<Integer> bufferIDs = new Vector<Integer>();
    protected static final int clockNode = 1990;
    protected static final int playersGroupNode = 1995;
    
    private Allocator krBusAllocator = new NaiveAllocator(0);
    private Allocator arBusAllocator = new NaiveAllocator(16); // start somewhere beyond the busses used for hardware i/o
    private Allocator nodeAllocator  = new NaiveAllocator(1000);
    private Allocator bufferAllocator= new NaiveAllocator(0);
    
    protected int beatBus;
    
    private static int bufferChannelsDefault = 7; 
    protected boolean recording = false;
    private static Hashtable<String,Integer> treeBufferIds = new Hashtable<String,Integer>();
    
    public SoundManager(SCAudio s) {
    	superCollider = s;
    	initialise();
    }
    
    private void initialise() {
    	beatBus = krBusAllocator.nextID();
    	
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"s_new","clockodile", clockNode, addToHead, 1, "out", beatBus}));
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"g_new", playersGroupNode, addToTail, 1}));
    }

    /**
     * Finds or loads the requested buffer of tree data.  
     */
    private int treeBuffer(String filename) throws IOException {
    	if (treeBufferIds.contains(filename)) {
    		return treeBufferIds.get(filename);
    	}
    	else {
    		int bufferId = bufferAllocator.nextID();
    		File treesDir = new File("/sdcard/",ScanVox.scanvoxTreeDirectory);
//    		File treesDir = new File(Environment.getExternalStorageDirectory(),ScanVox.scanvoxTreeDirectory);
    		File pathToTree = new File(treesDir,filename);
        	if(!sendOscAndWaitForDone(new OscMessage (new Object[] {
        			"b_allocRead",bufferId,pathToTree.getAbsolutePath()
        	}))) {
        		Log.e(TAG,"Could not load rtree buffer");
        		throw new IOException(String.format("Could not fetch tree data file '%s'",filename));
        	}
        	treeBufferIds.put(filename, bufferId);
        	return bufferId;
    	}
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
    	PlayingSound newSound = new PlayingSound(nodeAllocator, bufferAllocator,synthType);
    	int bufferChannels  = bufferChannelsDefault;  //
    	OscMessage bufferAllocMsg = new OscMessage( new Object[] {
    		"b_alloc",newSound.getRecordBuffer(),(int) ((size*SCAudio.sampleRateInHz/1000)/512),bufferChannels
    	});
    	Log.d(TAG,bufferAllocMsg.toString());
    	
    	if (!sendOscAndWaitForDone( bufferAllocMsg )) {
    		recording = false;
    		return -1;
    	}
    	OscMessage recordMsg = new OscMessage( new Object[] {
    	    "s_new","_scanvox_rec",newSound.getRecordNode(),addToHead,1,"timbrebuf",newSound.getRecordBuffer()
    	});
    	Log.d(TAG,recordMsg.toString());
    	superCollider.sendMessage( recordMsg );
    	addWhenReady(size, sac, bufferChannels, newSound);
    	return 0;
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
    		final long sleepTime, 
    		final SoundAddedCallback sac,
    		final int bufferChannels,
    		final PlayingSound newSound) {
		recordWait = new Thread(new Runnable(){
			public void run() {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {} 
				loopNew(sac,bufferChannels,newSound);
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
			SoundAddedCallback sac,
			int bufferChannels,
			PlayingSound newSound) {
		if (!recording) return;
		try {
			String playController;
			int curAmpBus         = krBusAllocator.nextID();
			int mappedControlsBus = krBusAllocator.nextIDs(newSound.synth.getNumControls());
			int curAudioBus       = arBusAllocator.nextID();
			playController = "_scanvox_playcontrols" + newSound.synth.getNumControls();
			Log.d(TAG, String.format("To control synth '%s', selected controller synth '%s'", newSound.synth.getLabel(), playController));
			OscMessage playMsg = new OscMessage( new Object[] {
	    	    "s_new",playController,newSound.getPlayNode(), addToHead, playersGroupNode,
	    	    "timbrebuf",   newSound.getRecordBuffer(),
	    	    "ampbus",      curAmpBus,
	    	    "controlsbus", mappedControlsBus,
	    	    "paramShouldBePitch",newSound.synth.getParamShouldBePitch(),
	    	    "treebuf",     treeBuffer(newSound.synth.getTreeFileName()),
	    	    "trevbuf",     treeBuffer(newSound.synth.getTrevmapFileName())
	    	});
			OscMessage beatMap = new OscMessage( new Object[] {
				"n_set",newSound.getPlayNode(),"clockbus",beatBus
			});
			OscMessage synthMessage = new OscMessage( new Object[] {
				"s_new","_maptsyn_ay1",newSound.getSynthNode(), addToTail, playersGroupNode,
	    	    "out",         curAudioBus,
			});
			OscMessage controlMap = new OscMessage( new Object[] {
				"n_mapn",newSound.getSynthNode(),2,mappedControlsBus,newSound.synth.getNumControls()
			});
			
			// create the amp mapping stuff using curAmpBus, before the increment happens
			// must send audio from lastAudioBus to 0, and must come AFTER the synth synth
			OscMessage ampMatchMsg = new OscMessage( new Object[] {
		    	    "s_new","_scanvox_ampmatch",newSound.getAmpMatchNode(), addAfter, newSound.getSynthNode(),
		    	    "soundsource",         curAudioBus,
		    	    "ampbus",      curAmpBus
		    	});

			//rm lastControlBusId += synthType.getNumControls() + 1; // skip enough for ampbus and the controlsses
			//rm lastAudioBusId++;
			Log.d(TAG,playMsg.toString());
	    	superCollider.sendMessage( playMsg );
	    	superCollider.sendMessage( beatMap );
	    	superCollider.sendMessage( synthMessage );
	    	superCollider.sendMessage( controlMap );
	    	superCollider.sendMessage( ampMatchMsg );
	    	//TODO - DEBUG, remove:
	    	superCollider.sendMessage( new OscMessage(new Object[]{"g_dumpTree", 0, 1}) );
	    	sac.whenSoundAdded ( newSound );
		} catch (IOException e) {
			Log.e(TAG, String.format("Error loading tree buffer '%s'",e.getMessage()));
		} finally {
			recording = false;
		}
	}
	
	/**
	 * Lightweight interface to asynchronously let the caller know that
	 * their sound was added successfully and gives them the ID of the 
	 * new sound.
	 * 
	 * @author Alex Shaw
	 *
	 */
	public interface SoundAddedCallback {
		public void whenSoundAdded(PlayingSound id);
	}

	public void removeSound(PlayingSound sound) {
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",sound.getPlayNode()
		}));
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",sound.getSynthNode()
		}));
	}

	/**
	 * Choose which tick to start the sound on
	 * 
	 * @param id the sound to modify
	 * @param f the tick to use
	 */
	public void setSoundStart(PlayingSound sound, int f) {
		OscMessage startMessage = new OscMessage(new Object[] {
				"n_set",sound.getPlayNode(),"myPhase",f
		});
		Log.d(TAG,startMessage.toString());
		superCollider.sendMessage(startMessage);
    	//TODO - DEBUG, remove:
    	superCollider.sendMessage( new OscMessage(new Object[]{"g_dumpTree", 0, 1}) );
	}

	/**
	 * Stops the currently created recording, if any.
	 * 
	 */
	public void stopRecording() {
		/*if (!recording) return;
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",recordNodeForId(lastBufferId)
		}));
		if (recordWait != null && recordWait.isAlive()) recordWait.interrupt();*/
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
	
	/**
	 * Set the BPM of the supercollider clock
	 * 
	 * @param bpm
	 */
	public void setBPM(int bpm) {
		float beatInHz = bpm/60f;
		OscMessage rateMessage = new OscMessage(new Object[] {
			"n_set",clockNode,"rate",beatInHz
		});
		Log.d(TAG,"RATE:"+rateMessage.toString());
		superCollider.sendMessage(rateMessage);
	}
}
