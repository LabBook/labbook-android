package uk.ac.ic.bss.labbook.backend;

import org.json.simple.JSONObject;

import uk.ac.ic.bss.labbook.LabBook;
import uk.ac.ic.bss.labbook.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class RegistrationActivity extends TrackedActivity {
	//private static final String TAG = RegistrationActivity.class.getName();

	//	private static final int DIALOG_SUCCESS = 1;
	private static final int DIALOG_FAILURE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.registration_dialog);
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button1:
			String email = ((EditText) findViewById(R.id.editText1)).getText().toString();
			new RegistrationTask().execute(email);
			setResult(RESULT_OK);
			break;
		case R.id.button2:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id, Bundle args) {
		String message = null;
		switch (id) {
		//		case DIALOG_SUCCESS:
		//			message = "Registration successful. " + (args.containsKey("confirmed") ?
		//					"Click 'OK' to restart the app." :
		//					"Please check your email to activate your device then click 'OK' to restart the app.");
		//			break;
		case DIALOG_FAILURE:
			message = args.getString("message");
			break;
		}
		return new AlertDialog.Builder(this)
		.setMessage(message)
		.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			//@Override
			public void onClick(DialogInterface arg0, int arg1) {
				switch (id) {
				//				case DIALOG_SUCCESS:
				//					Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
				//					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				//					startActivity(i);
				//					break;
				case DIALOG_FAILURE:
					finish();
					break;
				}
			}
		}).create();
	}

	private class RegistrationTask extends AsyncTask<String, Void, JSONObject> {
		@Override
		protected JSONObject doInBackground(String... args) {
			String email = args[0];
			//Patterns.EMAIL_ADDRESS.matcher(email).matches();
			Uri uri = Uri.parse(LabBook.serverProperties.getProperty("external.url")).buildUpon().appendPath("App").appendPath("register").build();
			String url = uri.buildUpon()
					.appendQueryParameter("email", email)
					.appendQueryParameter("device", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
					.appendQueryParameter("product", Build.PRODUCT)
					.build().toString();
			JSONObject result = (JSONObject) Util.json(url);
			if (!result.containsKey("error")) {
				String username = (String) result.get("username");
				String token = (String) result.get("token");
				LabBook.prefs
				.edit()
				.putString(LabBook.PREFS_EMAIL, email)
				.putString(LabBook.PREFS_TOKEN, token)
				.putString(LabBook.PREFS_USERNAME, username)
				.commit();
				//this feature doesn't require account confirmation (it is itself password protected and username presence implies an Imperial user)
				if (username != null) {
					getPackageManager().setComponentEnabledSetting(new ComponentName("uk.ac.ic.bss.labbook", "uk.ac.ic.bss.labbook.backend.CIFSActivity"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}
			}
			return result;
		};
		@Override
		protected void onPostExecute(JSONObject result) {
			if (result.containsKey("error")) {
				showDialog(DIALOG_FAILURE, Util.bundle((JSONObject) result.get("error")));
			} else {
				updateStatus(RegistrationActivity.this, new Handler.Callback() {
					@Override
					public boolean handleMessage(Message msg) {
						finish();
						return true;
					}
				});
				//showDialog(DIALOG_SUCCESS, Util.bundle(result));
			}
		}
	}

	public static void updateStatus(final Context context, final Handler.Callback callback) {
		if (getToken() != null && Util.isConnected(context)) {
			new AsyncTask<Void, Void, Object>() {
				@Override
				protected Object doInBackground(Void... params) {
					Uri uri = Uri.parse(LabBook.serverProperties.getProperty("external.url")).buildUpon().appendPath("App").appendPath("status").build();
					return Util.json(uri.toString(), getEmail(), getToken());
				}
				@Override
				protected void onPostExecute(Object json) {
					updateStatus(context, callback, json);
				}
			}.execute();
		}
	}

	private static void updateStatus(final Context context, final Handler.Callback callback, Object json) {
		if (json instanceof JSONObject && ((JSONObject) json).containsKey("status")) {
			String string = (String) ((JSONObject) json).get("status");
			Status status = string.length() == 0 ? null : Status.valueOf(string);
			boolean registered = isRegistered();
			Editor editor = LabBook.prefs.edit();
			if (status == null) {
				editor.remove(LabBook.PREFS_STATUS);
			} else {
				editor.putInt(LabBook.PREFS_STATUS, status.ordinal());
			}
			editor.commit();
			if (isUnconfirmed()) {
				new AlertDialog.Builder(context)
				.setMessage("Please check your mail to confirm your account then click OK to continue")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						updateStatus(context, callback);
					}
				})
				.setNegativeButton("Later", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						if (callback != null) {
							callback.handleMessage(null);
						}
					}
				})
				.show();
			} else if (registered != isRegistered()) {
				new AlertDialog.Builder(context)
				.setMessage("The app will now restart to complete registration")
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						Intent i = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						context.startActivity(i);
					}
				}).show();
			} else if (callback != null) {
				callback.handleMessage(null);
			}
		}
	}

	public static enum Status { ACTIVE, EXPIRED }
	private static Status getStatus() {
		int status = LabBook.prefs.getInt(LabBook.PREFS_STATUS, -1);
		return status == -1 ? null : Status.values()[status];
	}

	public static boolean isUnconfirmed() {
		return getToken() != null && getStatus() == null;
	}

	public static boolean isRegistered() {
		return Status.ACTIVE == getStatus();
	}

	public static boolean isExpired() {
		return Status.EXPIRED == getStatus();
	}

	public static String getToken() {
		return LabBook.prefs.getString(LabBook.PREFS_TOKEN, null);
	}

	public static String getUsername() {
		return LabBook.prefs.getString(LabBook.PREFS_USERNAME, null);
	}

	public static String getEmail() {
		return LabBook.prefs.getString(LabBook.PREFS_EMAIL, null);
	}
}
