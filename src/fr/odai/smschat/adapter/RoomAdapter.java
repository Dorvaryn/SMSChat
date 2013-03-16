package fr.odai.smschat.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import fr.odai.smschat.R;
import fr.odai.smschat.model.POJORoom;
import fr.odai.smschat.widget.HiddenQuickActionSetup;
import fr.odai.smschat.widget.SwipeableHiddenView;

public class RoomAdapter extends ArrayAdapter<POJORoom> {
	private HiddenQuickActionSetup mQuickActionSetup;
	
	public RoomAdapter(Context context, int textViewResourceId, ArrayList<POJORoom> rooms, HiddenQuickActionSetup setup) {
		super(context, textViewResourceId, rooms);
		mQuickActionSetup = setup;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			convertView = (SwipeableHiddenView) vi.inflate(
					R.layout.item_room, null);
			((SwipeableHiddenView) convertView).setHiddenViewSetup(mQuickActionSetup);

			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.item_list_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		POJORoom room = getItem(position);
		holder.name.setText(room.name);
		return convertView;
	}

	private class ViewHolder {
		public TextView name;
	}
}
