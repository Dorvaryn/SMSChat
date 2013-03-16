package fr.odai.smschat.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import fr.odai.smschat.FragementCallbacks;
import fr.odai.smschat.R;
import fr.odai.smschat.db.DBHelper;
import fr.odai.smschat.model.POJORoom;

public class RoomDetailFragment extends Fragment {

	private EditText name;
	private TextView infos;
	private POJORoom room;
	private FragementCallbacks mCallbacks = sDummyCallbacks;

	private static FragementCallbacks sDummyCallbacks = new FragementCallbacks() {
		@Override
		public long getListId() {
			return 0;
		}

		@Override
		public void updateTitle(String title) {
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View root = inflater.inflate(R.layout.fragment_chat_room_detail,
				container, false);
		name = (EditText) root.findViewById(R.id.editName);
		infos = (TextView) root.findViewById(R.id.infos);
		name.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				mCallbacks.updateTitle(s.toString());
			}
		});
		return root;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof FragementCallbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}
		mCallbacks = (FragementCallbacks) activity;
	}

	@Override
	public void onResume() {
		super.onResume();
		Context ctx = getActivity();
		room = DBHelper.getRoom(ctx, mCallbacks.getListId());
		name.setText(room.name);
		if (room.isInit(ctx)) {
			String format = ctx.getResources().getString(
					R.string.diffusion_list_textInfos_semi);
			String infosN = String.format(format, 0,
					room.getNbContacts(ctx));
			infos.setText(infosN);
		} else {
			infos.setText(R.string.diffusion_list_textInfos);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if (!name.getText().toString().equalsIgnoreCase("")) {
			room.name = name.getText().toString();
		}
		mCallbacks.updateTitle(room.name);
		DBHelper.updateRoom(getActivity(), room);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

}
