package fr.odai.smschat.fragment;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import fr.odai.smschat.FragementCallbacks;
import fr.odai.smschat.R;
import fr.odai.smschat.adapter.ContactAdapter;
import fr.odai.smschat.db.DBHelper;
import fr.odai.smschat.model.DisplayContact;
import fr.odai.smschat.utils.AndroidUtils;
import fr.odai.smschat.widget.HiddenQuickActionSetup;
import fr.odai.smschat.widget.HiddenQuickActionSetup.OnQuickActionListener;

public class ChatContactFragment extends ListFragment implements OnQuickActionListener{

		private static final String STATE_ACTIVATED_POSITION = "activated_position";

		private FragementCallbacks mCallbacks = sDummyCallbacks;
		private int mActivatedPosition = ListView.INVALID_POSITION;
		private ContactAdapter autoAdapter;

		private static final class QuickAction {
			public static final int CONFIRM = 1;
		}

		private HiddenQuickActionSetup mQuickActionSetup;
		private AutoCompleteTextView phoneNumber;
		
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
				setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
			}View root = inflater.inflate(R.layout.fragment_room_contact, container, false);

			setupQuickAction();
			phoneNumber = (AutoCompleteTextView) root.findViewById(R.id.text_contact);
			
			
			
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
				throw new IllegalStateException("Activity must implement fragment's callbacks.");
			}
			mCallbacks = (FragementCallbacks) activity;
		}

		
		@Override
		public void onResume() {
			super.onResume();
			ArrayList<DisplayContact> contacts = DBHelper.getDisplayContacts(getActivity(), mCallbacks.getListId());
			// List adapter
			final ContactAdapter adapter = new ContactAdapter(getActivity(), R.layout.item_contact, contacts, mQuickActionSetup);
			setListAdapter(adapter);
			
			//Purge the search adapter from all contacts already in this list
			//TODO: remove this by implementing a filterable search in database
			ArrayList<DisplayContact> allContacts = DisplayContact.getAllContacts(getActivity());
			for (Iterator<DisplayContact> iterator = allContacts.iterator(); iterator.hasNext();) {
				DisplayContact contact = (DisplayContact) iterator.next();
				for (Iterator<DisplayContact> iterator2 = contacts.iterator(); iterator2
						.hasNext();) {
					DisplayContact exists = (DisplayContact) iterator2.next();
					if(exists.phone.equalsIgnoreCase(contact.phone)){
						iterator.remove();
					}
				}
			}
			
			autoAdapter = new ContactAdapter(getActivity(), R.layout.item_contact,
					allContacts, mQuickActionSetup);
			

			phoneNumber.setAdapter(autoAdapter);
			phoneNumber.setOnItemClickListener(new OnItemClickListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> contactAdapter, View arg1, int position, long arg3) {
					DisplayContact newContact = autoAdapter.getItem(position);
					DBHelper.insertContact(getActivity(), mCallbacks.getListId(), newContact.phone);
					((ArrayAdapter<DisplayContact>) getListAdapter()).add(newContact);
					phoneNumber.setText("");
					autoAdapter.remove(newContact);
				}
			});	
			
			phoneNumber.setOnEditorActionListener(new AutoCompleteTextView.OnEditorActionListener() {
				@SuppressWarnings("unchecked")
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						if(!autoAdapter.isEmpty()){
							DisplayContact newContact = autoAdapter.getItem(0);
							DBHelper.insertContact(getActivity(), mCallbacks.getListId(), newContact.phone);
							((ArrayAdapter<DisplayContact>) getListAdapter()).add(newContact);
							phoneNumber.setText("");
						}
						return true;
					}
					return false;
				};
			});
		}

		@Override
		public void onDetach() {
			super.onDetach();
			mCallbacks = sDummyCallbacks;
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			if (mActivatedPosition != ListView.INVALID_POSITION) {
				outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
			}
		}

		public void setActivateOnItemClick(boolean activateOnItemClick) {
			getListView().setChoiceMode(
					activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		}

		public void setActivatedPosition(int position) {
			if (position == ListView.INVALID_POSITION) {
				getListView().setItemChecked(mActivatedPosition, false);
			} else {
				getListView().setItemChecked(position, true);
			}

			mActivatedPosition = position;
		}
	
	private void setupQuickAction() {
		Context ctx = getActivity();
		mQuickActionSetup = new HiddenQuickActionSetup(ctx);
		mQuickActionSetup.setOnQuickActionListener(this);

		int imageSize = AndroidUtils.dipToPixel(ctx, 40);
		
		// a nice cubic ease animation
		mQuickActionSetup.setOpenAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				v -= 1;
				return v * v * v + 1;
			}
		});
		mQuickActionSetup.setCloseAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				return v * v * v;
			}
		});

		mQuickActionSetup.setBackgroundResource(android.R.color.darker_gray);
		mQuickActionSetup.setImageSize(imageSize, imageSize);
		mQuickActionSetup.setAnimationSpeed(700);
		mQuickActionSetup.setStartOffset(AndroidUtils.dipToPixel(ctx, 20));
		mQuickActionSetup.setStopOffset(AndroidUtils.dipToPixel(ctx, 50));
		mQuickActionSetup.setStickyStart(false);
		mQuickActionSetup.setSwipeOnLongClick(true);

		mQuickActionSetup.setConfirmationMessage(QuickAction.CONFIRM,
				R.string.diffusion_contact_remove_confirm, R.drawable.ic_confirm,
				R.string.diffusion_contact_remove_message);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onQuickAction(AdapterView<?> parent, View view, int position,
			int quickActionId) {
		switch (quickActionId) {
		case QuickAction.CONFIRM:
			DisplayContact toDelete = (DisplayContact) getListAdapter().getItem(position);
        	((ArrayAdapter<DisplayContact>) getListAdapter()).remove(toDelete);
        	DBHelper.removeContact(getActivity(), mCallbacks.getListId(), toDelete.phone); 
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
			autoAdapter.add(toDelete);
			break;

		default:
			break;
		}
	}
	
}
