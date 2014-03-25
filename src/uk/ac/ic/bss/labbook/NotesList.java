package uk.ac.ic.bss.labbook;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import uk.ac.ic.bss.labbook.LabBook.Mode;
import uk.ac.ic.bss.labbook.backend.RegistrationActivity;
import uk.ac.ic.bss.labbook.backend.Util;
import uk.ac.ic.bss.labbook.backend.Version;
import uk.ac.ic.bss.labbook.models.Notebook;
import uk.ac.ic.bss.labbook.models.Notebooks;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;

public class NotesList extends TrackedListActivity {
	private static final int ACTIVITY_EDIT = 0;
	public static final String KEY_FOLDER = "FOLDER";
	public File labBookDir = LabBook.labBookDir;
	public static float pensize = 4F, erasersize = 30F;

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm");

	private NotebookArrayAdapter adapter;

	protected int filenum = 0;
	private List<CheckBox> checkboxvec = new ArrayList<CheckBox>();
	private String tag;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.notes_list);

		tag = LabBook.prefs.getString("tag", null);

		final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		updateTitle();
		actionBar.addAction(new Action() {
			@Override
			public void performAction(View view) {
				Notebook notebook = Notebooks.create(new File(labBookDir, UUID.randomUUID().toString()));
				if (tag != null) {
					notebook.addTag(tag);
				}
				adapter.add(notebook);
				adapter.sort(comparator);

				Intent i = new Intent(NotesList.this, LabBookActivity.class);
				i.putExtra(KEY_FOLDER, notebook.folder);
				startActivityForResult(i, ACTIVITY_EDIT);
			}
			@Override
			public int getDrawable() {
				return android.R.drawable.ic_menu_add;
			}
		});
		actionBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Notebooks.getTags().length > 0) {
					showFilterDialog();
				}
			}
		});

		if (!labBookDir.exists()) {
			if (labBookDir.mkdir()) {
				try {
					File welcome = new File(labBookDir, UUID.randomUUID().toString());
					welcome.mkdir();
					Util.unzip(getAssets().open("welcome.zip"), welcome);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				showAlert("Error", "SD card not present. Required for photo, video and audio capture");
			}
		}

		adapter = new NotebookArrayAdapter(this, new ArrayList<Notebook>());
		populateAdapter();
		setListAdapter(adapter);

		registerForContextMenu(getListView());

		final Handler.Callback updateStatus = new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				RegistrationActivity.updateStatus(NotesList.this, new Handler.Callback() {
					@Override
					public boolean handleMessage(Message msg) {
						Version.check(NotesList.this);
						return true;
					}
				});
				return true;
			}
		};

		if (!LabBook.prefs.getBoolean(LabBook.PREFS_ACCEPTED, false) && !LabBook.mode.equals(Mode.DEV)) {
			showTermsDialog(new Handler.Callback() {
				@Override
				public boolean handleMessage(Message msg) {
					updateStatus.handleMessage(null);
					return true;
				}
			});
		} else {
			updateStatus.handleMessage(null);
		}

		if (RegistrationActivity.isRegistered()) {
			AccountManager accountManager = AccountManager.get(this);
			if (accountManager.getAccountsByType(getString(R.string.account_type)).length == 0) {
				Account account = new Account(RegistrationActivity.getEmail(), getString(R.string.account_type));
				ContentResolver.setSyncAutomatically(account, getString(R.string.content_authority), true);
				ContentResolver.setIsSyncable(account, getString(R.string.content_authority), 1);
				AccountManager.get(this).addAccountExplicitly(account, RegistrationActivity.getToken(), null);
				Bundle extras = new Bundle();
				extras.putBoolean("scheduled", true);
				extras.putString("home", LabBook.labBookDir.getPath());
				ContentResolver.addPeriodicSync(account, getString(R.string.content_authority), extras, 24 * 60 * 60);
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Notebook notebook = adapter.getItem(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
		menu.setHeaderTitle(notebook.getName());
		menu.add(notebook.getTags().length == 0 ? "Add tag" : "Remove tag");
		menu.add("Delete");
	}

	@Override
	protected void onStop() {
		super.onStop();

		LabBook.prefs.edit()
		.putString("tag", tag)
		.commit();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
		Notebook notebook = adapter.getItem(position);
		if ("Delete".equals(item.getTitle())) {
			notebook.delete();
			adapter.remove(notebook);
		} else if ("Add tag".equals(item.getTitle())) {
			if (Notebooks.getTags().length > 0) {
				showAssignTagDialog(notebook);
			} else {
				showNewTagDialog(notebook);
			}
		} else if ("Remove tag".equals(item.getTitle())) {
			String removed = notebook.getTags()[0];
			notebook.removeTag(removed);
			if (removed.equals(tag)) {
				adapter.remove(notebook);
			}
			if (adapter.isEmpty() && tag != null) {
				tag = null;
				populateAdapter();
				updateTitle();
			} else {
				adapter.notifyDataSetChanged();
			}
			Util.sync(NotesList.this);
		}
		return true;
	}

	private void showTermsDialog(final Handler.Callback callback) {
		new AlertDialog.Builder(this)
		.setMessage("LabBook is currently in beta. During this period the app may send crash reports and anonymous usage statistics to our server. Is this ok?")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
				LabBook.prefs.edit().putBoolean(LabBook.PREFS_ACCEPTED, true).commit();
				callback.handleMessage(null);
			}
		})
		.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		})
		.show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, LabBookActivity.class);
		i.putExtra(KEY_FOLDER, adapter.getItem(position).folder);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch(requestCode) {
		case ACTIVITY_EDIT:
			adapter.sort(comparator);
			break;
		}
	}

	public void showAlert(String title, String result){
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(result)
		.setNegativeButton("OK", null)
		.show();
	}

	// To prevent crashes when screen orientation changes and onProgressDialog is showing.
	// Also, in manifest file have to addthe statement android:configChanges="keyboardHidden|orientation" for that activity.
	// so when your screen orientation is changed, it wont call the onCreate() method again.

	@Override
	public void onConfigurationChanged(Configuration arg0)
	{
		super.onConfigurationChanged(arg0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.main_menu, menu);
		if (RegistrationActivity.getToken() != null) {
			menu.removeItem(R.id.register);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.about:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.feedback:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:help@labbook.cc?subject=LabBook feedback")));
			break;
		case R.id.register:
			startActivity(new Intent(this, RegistrationActivity.class));
			break;
		case R.id.delete:
			deleteFilesDialog();
			break;
		}
		return true;
	}

	private void deleteFilesDialog(){
		LinearLayout ll = new LinearLayout(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT );
		ll.setOrientation(1);
		ll.setGravity(Gravity.CENTER);
		ll.setLayoutParams(lp);

		ScrollView s = new ScrollView(this);
		ll.addView(s);

		LinearLayout ll2 = new LinearLayout(this);
		ll2.setOrientation(1);
		ll2.setGravity(Gravity.CENTER);
		ll2.setLayoutParams(lp);

		s.addView(ll2);

		checkboxvec.clear();
		for (int i = 0; i < adapter.getCount(); i++) {
			Notebook notebook = adapter.getItem(i);
			CheckBox cb = new CheckBox(this);
			cb.setTag(notebook);
			cb.setChecked(false);
			cb.setText(notebook.getName());
			ll2.addView(cb);
			checkboxvec.add(cb);
		}

		new AlertDialog.Builder(this)
		.setTitle("Delete Notes")
		.setPositiveButton("Delete", new DialogInterface.OnClickListener() {  // Get Data
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				showDeleteFilesAlert();
			}
		})
		.setNegativeButton("Cancel", null)
		.setView(ll)
		.show();
	}

	public void deleteFiles() {
		for (CheckBox cb : checkboxvec) {
			if (cb.isChecked()) {
				Notebook notebook = (Notebook) cb.getTag();
				notebook.delete();
				adapter.remove(notebook);
			}
		}
		if (adapter.isEmpty() && tag != null) {
			tag = null;
			populateAdapter();
			updateTitle();
		}
	}

	private void showDeleteFilesAlert(){
		new AlertDialog.Builder(this)
		.setTitle("Confirm Delete")
		.setMessage("All data will be erased. Are you sure?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				deleteFiles();
			}
		})
		.setNegativeButton("No", null)
		.show();
	}

	private static Comparator<Notebook> comparator = new Comparator<Notebook>() {
		@Override
		public int compare(Notebook lhs, Notebook rhs) {
			return ((Long) rhs.getLastModified()).compareTo(lhs.getLastModified());
		}
	};

	private static class NotebookArrayAdapter extends ArrayAdapter<Notebook> {

		public NotebookArrayAdapter(Context context, List<Notebook> objects) {
			super(context, -1, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Notebook notebook = getItem(position);

			View itemView;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				itemView = inflater.inflate(R.layout.lb_cover_item, null);
			} else {
				itemView = convertView;
			}

			//			if (imageButton == null) {
			//				imageButton = (ImageButton) itemView.findViewById(R.id.icon);
			//				imageButton.setAdjustViewBounds(true);
			//				Drawable notebookDrawable = itemView.getResources().getDrawable(R.drawable.notebook);
			//				int reqWidth = notebookDrawable.getIntrinsicWidth();
			//				WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			//				int width = windowManager.getDefaultDisplay().getWidth();
			//				int inSampleSize = Math.round((float)width / (float)reqWidth);
			//				imageButton.setMaxWidth((int) (width / inSampleSize));
			//			}

			int pages = Math.max(1, notebook.txtdir.list().length);
			String description = String.format("%s%n%s%n%s page" + (pages > 1 ? "(s)" : ""), notebook.getName(), dateFormat.format(new Date(notebook.getLastModified())), pages);
			String[] tags = notebook.getTags();
			if (tags.length > 0) {
				description += String.format("%n%s", tags[0]);
			}
			TextView textView = ((TextView) itemView.findViewById(R.id.expNameText));
			textView.setText(description);
			textView.setFocusable(false);

			File thumbnail = new File(parent.getContext().getExternalCacheDir(), notebook.id);
			ImageButton imageButton = (ImageButton) itemView.findViewById(R.id.icon);
			if (thumbnail.exists()) {
				imageButton.setImageBitmap(BitmapFactory.decodeFile(thumbnail.getPath()));
			} else {
				imageButton.setImageResource(R.drawable.notebook);
			}
			imageButton.setFocusable(false);
			imageButton.setClickable(false);

			return itemView;
		}
	}

	private void showAssignTagDialog(final Notebook notebook) {
		String[] tags = Notebooks.getTags();
		Arrays.sort(tags);
		final String[] options = new String[tags.length + 1];
		options[0] = "New tag...";
		System.arraycopy(tags, 0, options, 1, tags.length);
		new AlertDialog.Builder(this)
		.setTitle("Assign tag")
		.setItems(options, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					showNewTagDialog(notebook);
				} else {
					notebook.addTag(options[which]);
					Util.sync(NotesList.this);
					adapter.notifyDataSetChanged();
				}
			}
		})
		.setPositiveButton("Ok", null).setNegativeButton("Cancel", null)
		.show();
	}

	private void showNewTagDialog(final Notebook notebook) {
		final EditText input = new EditText(this);
		input.setFilters(new InputFilter[] {
				new InputFilter.LengthFilter(10),
				new InputFilter() {
					@Override
					public CharSequence filter(CharSequence source, int start, int end,	Spanned dest, int dstart, int dend) {
						for (int i = start; i < end; i++) {
							if (!Character.isLetterOrDigit(source.charAt(i))) {
								return "";
							}
						}
						return null;
					}
				}});
		final AlertDialog alertDialog = new AlertDialog.Builder(this)
		.setTitle("New tag")
		.setView(input)
		.setPositiveButton("Ok", null)
		.setNegativeButton("Cancel", null)
		.create();
		alertDialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						String text = input.getText().toString();
						if (text.length() > 0) {
							notebook.addTag(text.toLowerCase());
							Util.sync(NotesList.this);
							adapter.notifyDataSetChanged();
							alertDialog.dismiss();
						} else {
							Toast.makeText(NotesList.this, "Tag cannot be empty", Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
		alertDialog.show();
	}

	private void showFilterDialog() {
		String[] tags = Notebooks.getTags();
		Arrays.sort(tags);
		final String[] options = new String[tags.length + 1];
		options[0] = "All tags";
		System.arraycopy(tags, 0, options, 1, tags.length);
		new AlertDialog.Builder(this)
		.setTitle("Filter")
		.setItems(options, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				tag = which == 0 ? null : options[which];
				populateAdapter();
				updateTitle();
			}
		})
		.setPositiveButton("Ok", null).setNegativeButton("Cancel", null)
		.show();
	}

	private void populateAdapter() {
		adapter.clear();
		for (File folder : labBookDir.listFiles(Util.directoryFilter)) {
			Notebook notebook = Notebooks.get(folder);
			if (tag == null || Arrays.asList(notebook.getTags()).contains(tag)) {
				adapter.add(Notebooks.get(folder));
			}
		}
		adapter.sort(comparator);
	}

	private void updateTitle() {
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		if (tag == null) {
			actionBar.setTitle(R.string.app_name);
		} else {
			actionBar.setTitle(String.format("%s (%s)", getString(R.string.app_name), tag));
		}
	}
}
