package org.isophonics.scanvox;

import java.util.Vector;

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
    protected SCAudio superCollider;
    protected Vector<Integer> bufferIDs = new Vector<Integer>();
    protected int lastBufferId = -1; // NB: !== bufferIDs.size()-1, due to possible deletions

    public SoundManager(SCAudio s) {
    	superCollider = s;
    }

    public void recordNew(long size) {
    	superCollider.sendMessage( new OscMessage( new Object[] {
    		"b_alloc",++lastBufferId,size
    	}));
    }
}
