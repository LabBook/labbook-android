package uk.ac.ic.bss.labbook.backend;

import uk.ac.ic.bss.labbook.LabBook;
import uk.ac.ic.bss.labbook.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;

public class Version {
	//private static final String TAG = Version.class.getName();

	public static int versionCode(final Activity activity) {
		try {
			return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			return -1;
		}
	}

	public static String versionName(final Activity activity) {
		try {
			return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	public static void check(final Activity activity) {
		if (LabBook.mode == LabBook.Mode.TEST && Util.isConnected(activity)) {
			final Uri uri = Uri.parse(Util.getProperty(activity.getResources(), R.raw.server, "external.url"));
			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Void... params) {
					String url = uri.buildUpon().appendPath("App").appendPath("properties").toString();
					String serverVersion = Util.properties(url).getProperty("versionCode");
					return serverVersion != null && versionCode(activity) < Integer.parseInt(serverVersion.trim());
				}
				@Override
				protected void onPostExecute(Boolean result) {
					if (result) {
						new AlertDialog.Builder(activity)
						.setMessage("A new version of the app is available. Download now? ")
						.setCancelable(false)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							//@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								Uri url = uri.buildUpon().appendPath("App").appendPath("download").build();
								activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(url));
							}
						})
						.setNegativeButton("Later", new DialogInterface.OnClickListener() {
							//@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).show();
					}
				}
			}.execute();
		}
	}
}
