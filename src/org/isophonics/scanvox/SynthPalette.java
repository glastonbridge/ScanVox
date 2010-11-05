package org.isophonics.scanvox;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Let the user choose synths from a palette.
 * 
 * TODO: move the other synthpallete code in here
 * @author alex
 *
 */
public class SynthPalette extends ArrayAdapter<MappedSynth> {

	private MappedSynth[] synthList;
	public SynthPalette(Context context, int simpleListItem1,
			MappedSynth[] synthList) {
		super(context, simpleListItem1,synthList);
		this.synthList = synthList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    View retView = super.getView(position,convertView,parent);
	    retView.setBackgroundColor(synthList[position].getGuiColour());
	    return retView;
	}
	
	public interface Handler {
		public void selected(int choice);
	}
}
