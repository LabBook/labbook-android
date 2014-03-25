package uk.ac.ic.bss.labbook;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.annotation.ReportsCrashes;

import uk.ac.ic.bss.labbook.backend.RegistrationActivity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

@ReportsCrashes(formKey = "dGo5R2FwZnBHc2FlUjk4a2pmaXI5clE6MQ")
public class LabBook extends Application {

	private static final String TAG = LabBook.class.getName();
	public static Mode mode;
	public static SharedPreferences prefs;
	public static final String PREFS_ACCEPTED = "ACCEPTED";
	public static final String PREFS_PASSWORD = "PASSWORD";
	public static final String PREFS_USERNAME = "USERNAME";
	public static final String PREFS_EMAIL = "EMAIL";
	public static final String PREFS_TOKEN = "TOKEN";
	public static final String PREFS_STATUS = "STATUS";
	//this is a hack to cache the cifs password without saving it to disk
	public static String cifsPassword;
	public static File labBookDir;
	public static Properties serverProperties;

	@Override
	public void onCreate() {
		mode = getMode();

		if (mode != Mode.DEV) {
			ACRA.init(this);
		}

		prefs = getSharedPreferences(this.getClass().getName(), Context.MODE_PRIVATE);
		ErrorReporter.getInstance().putCustomData("email", RegistrationActivity.getEmail());

		labBookDir = new File(Environment.getExternalStorageDirectory(), "LabBook");

		serverProperties = new Properties();
		try {
			serverProperties.load(getResources().openRawResource(R.raw.server));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		super.onCreate();
	}

	public static enum Mode { DEV, TEST, PROD }
	private Mode getMode() {
		if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
			return Mode.DEV;
		}
		try {
			Class.forName(getString(R.string.doNotDelete));
			Log.i(TAG, Build.PRODUCT + " " + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
			return Mode.TEST;
		} catch (ClassNotFoundException e) {
			return Mode.PROD;
		}
	}
}
