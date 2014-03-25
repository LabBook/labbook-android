package uk.ac.ic.bss.labbook.backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ic.bss.labbook.R;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;

public class CIFSActivity extends TrackedListActivity {
	private static final String TAG = CIFSActivity.class.getName();

	private static final int AUTHENTICATED = 0;

	private static Uri uri;

	private String username;
	private String password;
	private String path = "/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		uri = Uri.parse(Util.getProperty(getResources(), R.raw.server, "internal.url")).buildUpon().appendPath("Application").appendPath("cifs").build();

		startActivityForResult(new Intent(this, AuthActivity.class), AUTHENTICATED);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == AUTHENTICATED) {
			if (resultCode == RESULT_OK) {
				username =  data.getStringExtra("username");
				password = data.getStringExtra("password");
				run();
			} else {
				finish();
			}
		}
	}

	private void run() {
		setListAdapter(new ArrayAdapter<String>(CIFSActivity.this, android.R.layout.simple_list_item_1, new String[] {"Loading..."}) {
			@Override
			public boolean isEnabled(int position) {
				return false;
			}
		});
		new AsyncTask<Void, Void, List<String>>() {
			@Override
			protected JSONArray doInBackground(Void... arg0) {
				JSONArray result = null;
				//TODO caching
				Log.d(TAG, "Retrieving metadata...");
				Object response = Util.json(uri.buildUpon().appendQueryParameter("path", path).toString(), username, password);
				//TODO cleaner exception handling
				if (response instanceof JSONArray) {
					Log.d(TAG, "Metadata retrieved from network");
					result = (JSONArray) response;
				} else {
					Log.e(TAG, "Error retrieving metadata: " + ((JSONObject) response).get("error"));
				}
				return result;
			};
			@Override
			protected void onPostExecute(List<String> result) {
				if (result == null) {
					setResult(RESULT_CANCELED);
					finish();
				} else {
					List<FileEntry> entries = new ArrayList<FileEntry>();
					if (!"/".equals(path)) {
						entries.add(new FileEntry(new File(path).getParent() + "/", "..", true));
					}
					for (String filepath : result) {
						boolean isDirectory = filepath.endsWith("/");
						try {
							if (isDirectory || new IntentFilter(Intent.ACTION_PICK, getIntent().getType()).hasDataType(Util.getMimeType(new File(filepath)))) {
								entries.add(new FileEntry(path + filepath, filepath, isDirectory));
							}
						} catch (MalformedMimeTypeException e) {
							Log.e(TAG, e.getMessage(), e);
						}
					}
					setListAdapter(new ArrayAdapter<FileEntry>(CIFSActivity.this, android.R.layout.simple_list_item_1, entries));
				}
			}
		}.execute();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final FileEntry entry = (FileEntry) getListView().getItemAtPosition(position);

		if (entry.is_dir) {
			path = entry.path;
			run();
		} else {
			new AsyncTask<Void, Void, File>() {
				@Override
				protected File doInBackground(Void... arg0) {
					try {
						File tempFile = new File(getCacheDir(), entry.fileName);
						Util.get(uri.buildUpon().appendQueryParameter("path", entry.path).toString(), username, password, tempFile);
						return tempFile;
					} catch (IOException e) {
						Log.e(TAG, "Error retrieving data: " + e.getMessage(), e);
					}
					return null;
				}
				@Override
				protected void onPostExecute(File tempFile) {
					if (tempFile != null) {
						Intent result = new Intent();
						result.setData(Uri.fromFile(tempFile));
						setResult(RESULT_OK, result);
					} else {
						setResult(RESULT_CANCELED);
					}
					finish();
				};
			}.execute();
		}
	}
}
