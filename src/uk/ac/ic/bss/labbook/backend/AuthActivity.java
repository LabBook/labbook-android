package uk.ac.ic.bss.labbook.backend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import uk.ac.ic.bss.labbook.LabBook;
import uk.ac.ic.bss.labbook.R;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class AuthActivity extends TrackedActivity {
	private static final String TAG = AuthActivity.class.getName();

	private static boolean savePassword = false;
	private static Uri uri;

	private String username;
	private String password;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		username = RegistrationActivity.getUsername();
		password = LabBook.cifsPassword;
		if (password == null) {
			password = LabBook.prefs.getString(LabBook.PREFS_PASSWORD, null);
		}

		try {
			Properties properties = new Properties();
			properties.load(getResources().openRawResource(R.raw.server));
			uri = Uri.parse(properties.getProperty("internal.url")).buildUpon().appendPath("Application").appendPath("index").build();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		run();
	}

	private void run() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... arg0) {
				boolean ok = false;
				if (username != null && password != null) {
					try {
						HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
						Util.auth(connection, username, password);
						ok = HttpURLConnection.HTTP_OK == connection.getResponseCode();
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
					}
				}
				return ok;
			};
			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					storeCredentials();
					Intent intent = new Intent();
					intent.putExtra("username", username);
					intent.putExtra("password", password);
					setResult(RESULT_OK, intent);
					finish();
				} else {
					setContentView(R.layout.cifs_credentials_dialog);
					if (username != null) {
						((EditText) findViewById(R.id.editText1)).setText(username);
						((EditText) findViewById(R.id.editText2)).requestFocus();
					}
					findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
						//@Override
						public void onClick(View v) {
							username = ((EditText) findViewById(R.id.editText1)).getText().toString();
							password = ((EditText) findViewById(R.id.editText2)).getText().toString();
							run();
						}
					});
					findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
						//@Override
						public void onClick(View v) {
							setResult(RESULT_CANCELED);
							finish();
						}
					});
				}
			}
		}.execute();
	}

	private void storeCredentials() {
		if (savePassword) {
			LabBook.prefs.edit().putString(LabBook.PREFS_PASSWORD, password).commit();
		} else {
			LabBook.cifsPassword = password;
		}
	}
}
