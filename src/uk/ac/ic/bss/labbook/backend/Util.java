package uk.ac.ic.bss.labbook.backend;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import uk.ac.ic.bss.labbook.LabBook;
import uk.ac.ic.bss.labbook.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class Util {
	private static final String TAG = Util.class.getName();

	public static String getProperty(Resources resources, int resource, String name) {
		String property = null;

		try {
			Properties properties = new Properties();
			properties.load(resources.openRawResource(resource));
			property = properties.getProperty(name);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return property;
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		copy(input, output, true);
	}

	public static void copy(InputStream input, OutputStream output, boolean closeOutput) throws IOException {
		copy(input, output, closeOutput, true);
	}

	public static void copy(InputStream input, OutputStream output, boolean closeOutput, boolean closeInput) throws IOException {
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		if (closeInput) {
			input.close();
		}
		if (closeOutput) {
			output.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Object json(String url, String username, String password) {
		JSONObject error;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestProperty("Accept", "application/json");
			auth(connection, username, password);
			if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
				return JSONValue.parse(new InputStreamReader(connection.getInputStream()));
			} else {
				error = (JSONObject) JSONValue.parse(new InputStreamReader(connection.getErrorStream()));
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			error = new JSONObject();
			error.put("type", e.getClass().getName());
			error.put("message", e.getMessage());
		}
		JSONObject json = new JSONObject();
		json.put("error", error);
		return json;
	}

	public static void get(String url, String username, String password, File dest) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		auth(connection, username, password);
		copy(connection.getInputStream(), new FileOutputStream(dest));
	}

	public static Object json(String url) {
		return json(url, null, null);
	}

	//	public static String text(String url) {
	//		try {
	//			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	//			connection.setRequestProperty("Accept", "text/plain");
	//			if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
	//				return new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
	//			}
	//		} catch (MalformedURLException e) {
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		return null;
	//	}

	public static Properties properties(String url) {
		try {
			URLConnection connection = new URL(url).openConnection();
			Properties properties = new Properties();
			properties.load(connection.getInputStream());
			return properties;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void auth(URLConnection connection, String username, String password) {
		if (username != null && password != null) {
			connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(), Base64.NO_WRAP));
		}
	}

	public static Bundle bundle(@SuppressWarnings("rawtypes") Map map) {
		Bundle bundle = new Bundle();
		for (Object key : map.keySet()) {
			bundle.putSerializable((String) key, (Serializable) map.get(key));
		}
		return bundle;
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return networkInfo != null && networkInfo.isConnected();
	}

	public static void sync(Context context) {
		Account[] accounts = AccountManager.get(context).getAccountsByType(context.getString(R.string.account_type));
		if (accounts.length > 0) {
			Bundle extras = new Bundle();
			extras.putBoolean("requested", true);
			extras.putString("home", LabBook.labBookDir.getPath());
			ContentResolver.requestSync(accounts[0], context.getString(R.string.content_authority), extras);
		}
	}

	public static class Barcode {
		public String description;
		public Bitmap thumbnail;
		public String manual;
	}
	//this should never be called on the main thread
	public static Barcode lookupBarcode(String barcode, Resources resources) {
		Barcode barcodeObj = null;
		final Uri uri = Uri.parse(Util.getProperty(resources, R.raw.server, "external.url"));
		String url = uri.buildUpon().appendPath("Barcodes").appendPath("thing").appendQueryParameter("barcode", barcode).build().toString();
		JSONObject thing = (JSONObject) Util.json(url);
		if (thing.containsKey("description")) {
			barcodeObj = new Barcode();
			barcodeObj.description = (String) thing.get("description");
			barcodeObj.manual = (String) thing.get("manual");
			byte[] thumbnailEncoded = Base64.decode((String) thing.get("thumbnail"), Base64.DEFAULT);
			barcodeObj.thumbnail = BitmapFactory.decodeByteArray(thumbnailEncoded, 0, thumbnailEncoded.length);
		}
		return barcodeObj;
	}

	public static String getMimeType(File file) {
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString()));
	}

	public static String getExtension(File file) {
		return MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString());
	}

	public static String read(File file) {
		try {
			return new Scanner(file).useDelimiter("\\A").next();
		} catch (NoSuchElementException e) {
			return "";
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	public static FileFilter directoryFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};

	public static boolean isObfuscated(Activity activity) {
		try {
			Class.forName(activity.getString(R.string.doNotDelete));
			return true;
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

	public static String lookup(String s, Resources resources) {
		try {
			final Uri uri = Uri.parse(Util.getProperty(resources, R.raw.server, "external.url"));
			String url = uri.buildUpon().appendPath("App").appendPath("lookup").appendQueryParameter("s", s).build().toString();
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			if (HttpURLConnection.HTTP_MOVED_TEMP == connection.getResponseCode()) {
				return connection.getHeaderField("Location");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void convert(Uri serverUri, File in, File out) throws IOException {
		Uri uri = serverUri.buildUpon().appendPath("App").appendPath("convert").build();
		HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
		String boundary = UUID.randomUUID().toString();
		connection.setRequestProperty("Content-Type", String.format("multipart/form-data; boundary=%s", boundary));
		connection.setDoOutput(true);
		write(connection, "--%s", boundary);
		write(connection, "Content-Disposition: form-data; name=\"file\"; filename=\"%s\"", in.getName());
		write(connection, "Content-Type: application/octet-stream");
		write(connection, "");
		write(connection, in);
		write(connection, "");
		write(connection, "--%s--", boundary);
		//int responseCode = connection.getResponseCode();
		copy(connection.getInputStream(), new FileOutputStream(out));
	}

	private static void write(URLConnection connection, String format, Object...args) throws IOException {
		connection.getOutputStream().write(String.format(format + "\r\n", args).getBytes());
	}

	private static void write(URLConnection connection, File file) throws IOException {
		Util.copy(new FileInputStream(file), connection.getOutputStream(), false);
	}

	public static void unzip(InputStream in, File folder) throws IOException {
		ZipInputStream zin = new ZipInputStream(in);
		ZipEntry entry;
		while ((entry = zin.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				new File(folder, entry.getName()).mkdirs();
			} else {
				copy(zin, new FileOutputStream(new File(folder, entry.getName())), true, false);
			}
		}
		in.close();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromFile(String pathName, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(pathName, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(pathName, options);
	}

}
