/*
  This file is part of ScanVox
  (c) 2010 Queen Mary University of London

    ScanVox is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ScanVox is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ScanVox.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.isophonics.scanvox;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import org.isophonics.scanvox.SCMessageManager.OscListener;
import org.isophonics.scanvox.allocators.Allocator;
import org.isophonics.scanvox.allocators.NaiveAllocator;

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
	public static final int CLOCK_TRIGGER_UID = 24;
	private static final String TAG = "SoundManager";
	private static final int addToHead = 0;
	private static final int addToTail = 1;
	private static final int addBefore = 2;
	private static final int addAfter  = 3;
	@SuppressWarnings("unused")
	private static final int addReplace= 4;
    protected SCAudio superCollider;
    protected Vector<Integer> bufferIDs = new Vector<Integer>();
    protected static final int clockNode = 1990;
    protected static final int recordersGroupNode = 1992;
    protected static final int playersGroupNode = 1995;
    
    private Allocator krBusAllocator = new NaiveAllocator(0);
    private Allocator arBusAllocator = new NaiveAllocator(16); // start somewhere beyond the busses used for hardware i/o
    private Allocator nodeAllocator  = new NaiveAllocator(1000);
    private Allocator bufferAllocator= new NaiveAllocator(0);
    
    protected int beatBus;
    private SCMessageManager messageManager;
    
    private static int bufferChannelsDefault = 7; 
    protected boolean recording = false;
    private static Hashtable<String,Integer> treeBufferIds = new Hashtable<String,Integer>();
    
    public SoundManager(SCAudio s, SCMessageManager messageManager) {
    	this.messageManager = messageManager;
    	superCollider = s;
    	initialise();
    }
    
    private void initialise() {
    	beatBus = krBusAllocator.nextID();
    	
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"notify", 1}));
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"s_new","clockodile", clockNode, addToHead, 1, 
    			"out", beatBus, 
    			"tr_uid", CLOCK_TRIGGER_UID}));
    	superCollider.sendMessage(new OscMessage( new Object[] {
    			"g_new", recordersGroupNode, addToTail, 1}));
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
     * Call back to someone when the status of the recording changes.
     * This allows a much more precise representation of the record
     * state to be displayed to the user.
     * 
     * @author alex
     *
     */
    public interface RecordListener {
		public void recordStart(PlayingSound s);
		public void recordUpdate();  // notify there's a new sample in the sound
		public void recordEnd();
	}

    /**
     * Any failure in the interaction between this class and SCAudio
     * @author alex
     *
     */
    public class SoundManagerException extends Exception {
		public SoundManagerException(String what) {
			super(what); // i say!
		}
		private static final long serialVersionUID = -2731210613186992589L;
    }
    
    /**
     * Create a buffer of the requested size, and fill it with audio from the mic.
     * Add it to the loop as soon as it's done.  Does all this in a separate thread
     * so as not to block GUI interaction.
     * 
     * @param size in ms
     * @param sac notify that the sound has been added
     * @param synthType 
     */
    public void recordNew (
    		final int size, 
    		final SoundAddedCallback sac, 
    		final MappedSynth synthType, 
    		final RecordListener recordListener) {
    	if (recording) return;
    	recording = true;
    	new Thread(new Runnable() {
    		public void run() {
    			try {
    				/**
    				 * How to add a sound, in fixe simple steps:
    				 */
    				PlayingSound newSound = allocateBuffer(size, synthType);
    				record(newSound, recordListener);
    				startPlayback(newSound);
    				addSynth(newSound);
    		    	sac.whenSoundAdded ( newSound );
    			} catch (Exception e) {
    				e.printStackTrace();
    			} finally {
    				recording = false;
    			}
    		}
    	}).start();
    }
    
    /**
     * Create a buffer, and associate it with a new PlayingSound
     * 
     * Stage 1 in the record process.
     * 
     * @param size
     * @return
     * @throws SoundManagerException 
     */
    public PlayingSound allocateBuffer(int size, MappedSynth synthType) throws SoundManagerException {
    	recording = true;
    	long ampArrayLen = ((SCAudio.sampleRateInHz * size) / 512) /4000; // 512 is hop size of features, 4 is decimation of db-samplerate in recsynth, 1000 is millisecs to secs 
    	PlayingSound newSound = new PlayingSound(nodeAllocator, bufferAllocator, krBusAllocator, arBusAllocator, synthType, (int)ampArrayLen);
    	newSound.length = size;
    	int bufferChannels  = bufferChannelsDefault;  //
    	OscMessage bufferAllocMsg = new OscMessage( new Object[] {
    		"b_alloc",newSound.getRecordBuffer(),(int) ((newSound.length*SCAudio.sampleRateInHz/1000)/512),bufferChannels
    	});
    	Log.d(TAG,bufferAllocMsg.toString());
    	
    	if (!sendOscAndWaitForDone( bufferAllocMsg )) {
    		throw new SoundManagerException("Could not allocate a buffer!");
    	}
    	return newSound;
    }
    
    /**
     * Tells the looper to start playing the requested buffer as soon as
     * it is ready (recording has finished).
     * @param recordListener 
     * 
     * @param bufferId
     */
    private void record(
    		final PlayingSound newSound, 
    		final RecordListener recordListener) {
    	// tr
    	SCMessageManager.OscListener trListener = new OscListener() {
    		public void receive(OscMessage msgFromServer) {
				if(((Integer)msgFromServer.get(1)).intValue()==newSound.getRecordNode()){
					// messagetype and node ID matches. 
					// to distinguish phase trigs and amp trigs, we also check that msg.get(2) matches the trigger ID
					switch(((Integer)msgFromServer.get(2)).intValue()){
					case 7:
						// It's a decibel amplitude message
						float dbamp = ((Float)msgFromServer.get(3)).floatValue();
						//Log.d(TAG, "addWhenReady found dbamp value: " + dbamp);
						newSound.pushDbampValue(dbamp);
						recordListener.recordUpdate();
						break;
					case 5:
						// It's a startingphase message
						newSound.phase = ((Float)msgFromServer.get(3)).floatValue();
						Log.d(TAG, "Set phase based on what clock said, as " + newSound.phase);
						break;
					}
				}
    		}
    	};
    	// n_end
    	SCMessageManager.OscListener endListener = new OscListener() {
    		public void receive(OscMessage msgFromServer) {
				if (((Integer)msgFromServer.get(1)).intValue()==newSound.getRecordNode()){
					Log.d(TAG, "addWhenReady discovered our own record synth has freed: " + newSound.getRecordNode());
					Log.d(TAG, "collected intamps: " + Arrays.toString(newSound.intamps));
					synchronized(SoundManager.this) {
						SoundManager.this.notify();
					}
				}
    		}
    	};

    	messageManager.register(trListener, "/tr");
    	messageManager.register(endListener, "/n_end");
    	OscMessage recordMsg = new OscMessage( new Object[] {
        	    "s_new","_scanvox_rec",newSound.getRecordNode(),addToHead,recordersGroupNode,
        	    	"timbrebuf", newSound.getRecordBuffer(),
        	    	"clockbus", beatBus
        	});
    	Log.d(TAG,recordMsg.toString());
    	recordListener.recordStart(newSound);
    	superCollider.sendMessage( recordMsg );
    	try {
    		synchronized(this) {
    			wait();
    		}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
    	recordListener.recordEnd();
		messageManager.unregister(trListener, "/tr");
		messageManager.unregister(endListener, "/n_end");
	}
    
    /**
     * Set up the housekeeping for the sound
     * 
     * @param bufferId
     * @throws IOException 
     */
	public void startPlayback(
			PlayingSound newSound) throws IOException {
		OscMessage makeGroupMessage = new OscMessage( new Object[] {
				"g_new", newSound.getPlayGroupNode(), addToTail, playersGroupNode
			});
		// NB unlike the other synths, the supervisor must not be added as a child of the playGroup - instead use addBefore  
		OscMessage supervisorMsg = new OscMessage( new Object[] {
	    	    "s_new","_scanvox_supervisor",newSound.getSupervisorNode(), addBefore, newSound.getPlayGroupNode(),
	    	    "myphase", 	   newSound.phase,
	    	    "tgtgroupid",  newSound.getPlayGroupNode(),
	    	    "timbrebuf",   newSound.getRecordBuffer(), // timbrebuf is passed in so it can know duration
	    	    "clockbus",    beatBus,
	    	    "trigbus",     newSound.getTrigBus()
	    	});
		
		//rm lastControlBusId += synthType.getNumControls() + 1; // skip enough for ampbus and the controlsses
		//rm lastAudioBusId++;
		superCollider.sendMessage( makeGroupMessage);
		superCollider.sendMessage( supervisorMsg);
    	//TODO - DEBUG, remove:
    	//superCollider.sendMessage( new OscMessage(new Object[]{"g_dumpTree", 0, 1}) );
	}
	
	/**
	 * Adds a synthesizer to be controlled by a playingsound,
	 * based on the MappedSynth associated with it.
	 */
	public void addSynth(
			PlayingSound newSound) throws IOException {

		String playController;
		playController = "_scanvox_playcontrols" + newSound.synth.getNumControls();
		Log.d(TAG, String.format("To control synth '%s', selected controller synth '%s'", newSound.synth.getLabel(), playController));
		OscMessage playMsg = new OscMessage( new Object[] {
	    	    "s_new",playController,newSound.getPlayNode(), addToHead, newSound.getPlayGroupNode(),
	    	    "timbrebuf",   newSound.getRecordBuffer(),
	    	    "ampbus",      newSound.getAmpBus(),
	    	    "controlsbus", newSound.getControlsBus(),
	    	    "paramShouldBePitch",newSound.synth.getParamShouldBePitch(),
	    	    "treebuf",     treeBuffer(newSound.synth.getTreeFileName()),
	    	    "trevbuf",     treeBuffer(newSound.synth.getTrevmapFileName()),
	    	    "trigbus",     newSound.getTrigBus()
	    	});

		OscMessage synthMessage = new OscMessage( new Object[] {
			"s_new",newSound.synth.getSynthDefName(),newSound.getSynthNode(), addToTail, newSound.getPlayGroupNode(),
    	    "out",         newSound.getAudioBus(),
		});
		OscMessage controlMap = new OscMessage( new Object[] {
			"n_mapn",newSound.getSynthNode(),2,newSound.getControlsBus(),newSound.synth.getNumControls()
		});		

		// create the amp mapping stuff using curAmpBus, before the increment happens
		// must send audio from lastAudioBus to 0, and must come AFTER the synth synth
		OscMessage ampMatchMsg = new OscMessage( new Object[] {
	    	    "s_new","_scanvox_ampmatch",newSound.getAmpMatchNode(), addAfter, newSound.getSynthNode(),
	    	    "soundsource",         newSound.getAudioBus(),
	    	    "ampbus",      newSound.getAmpBus()
	    	});
		
		superCollider.sendMessage( playMsg );	
    	superCollider.sendMessage( synthMessage );
    	superCollider.sendMessage( ampMatchMsg );
    	superCollider.sendMessage( controlMap );
	}

	/**
	 * Get rid of the current playing and synthesis components
	 * without removing buffers.  This is intended for use in swapping
	 * out synthesizers.
	 */
	public void removeSynth(PlayingSound sound) {
		removeNode(sound.getSynthNode());
		removeNode(sound.getAmpMatchNode());
		removeNode(sound.getPlayNode());
	}
	
	private void removeNode(int node) {
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free", node
		}));
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
				"n_free",sound.getSupervisorNode()
		}));
		superCollider.sendMessage(new OscMessage(new Object[] {
				"n_free",sound.getPlayGroupNode()
		}));
	}

	/**
	 * Choose which tick to start the sound on
	 * 
	 * @param id the sound to modify
	 * @param f the tick to use
	 */
	public void setSoundStart(PlayingSound sound, float f) {
		OscMessage startMessage = new OscMessage(new Object[] {
				"n_set",sound.getSupervisorNode(),"myphase",f
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

	private class WaitListener implements SCMessageManager.OscListener {
		protected boolean success = false;
		@Override
		public void receive(OscMessage msgFromServer) {
			String type = (String) msgFromServer.get(0);
			if (type.equals("/fail") || type.equals("/done")) {
				if (type.equals("/done")) success = true;
				synchronized(SoundManager.this) {
					SoundManager.this.notify();
				}
			}
		}
	}
	private boolean sendOscAndWaitForDone(OscMessage msg) {
		WaitListener waitListener = new WaitListener();
		messageManager.register(waitListener, "/fail");
		messageManager.register(waitListener, "/done");
    	superCollider.sendMessage( msg );
    	synchronized(this) {
    		try {
				wait(50000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
		messageManager.unregister(waitListener, "/fail");
		messageManager.unregister(waitListener, "/done");
		return waitListener.success;
		
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
