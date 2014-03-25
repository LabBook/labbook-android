package uk.ac.ic.bss.labbook.backend.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ic.bss.labbook.R;
import uk.ac.ic.bss.labbook.backend.Util;
import uk.ac.ic.bss.labbook.models.Notebooks;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final int SYNCING = 0;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		if (extras.isEmpty()) {
			return;
		}

		File home = new File(extras.getString("home"));
		//turn off selective sync until we handle notebook deletion with it turned on
		String notebook = null;
		String email = account.name;
		String token = AccountManager.get(getContext()).getPassword(account);

		if (!home.exists()) {
			return;
		}

		Notification notification = new Notification(android.R.drawable.stat_notify_sync, null, System.currentTimeMillis());
		notification.setLatestEventInfo(getContext().getApplicationContext(), getContext().getString(R.string.app_name), "Sync in progress...", PendingIntent.getActivity(getContext(), 0, new Intent(), 0));
		NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(SYNCING, notification);

		Uri uri = Uri.parse(Util.getProperty(getContext().getResources(), R.raw.server, "external.url")).buildUpon().appendPath("Sync").build();
		try {
			Map<String, Long> checksums = new HashMap<String, Long>();
			checksums(notebook != null ? new File(home, notebook) : home, home, checksums);
			filter(checksums, home);
			if (diff(checksums, notebook, uri, email, token) && sync(checksums, uri, email, token, home, extras.getBoolean("scheduled"))) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			notificationManager.cancel(SYNCING);
		}

		//disable failure notification until we figure out how to display it on the ui thread
		//Toast.makeText(getContext().getApplicationContext(), String.format("Synchronization failed"), Toast.LENGTH_LONG).show();
		syncResult.stats.numIoExceptions = 1;
	}

	private static Map<String, Long> checksums(File folder, File home, Map<String, Long> checksums) throws IOException {
		for (File file : folder.listFiles(new FilenameFilter() {
			//@Override
			public boolean accept(File dir, String filename) {
				return !filename.startsWith(".");
			}
		})) {
			if (file.isDirectory()) {
				checksums(file, home, checksums);
			} else {
				checksums.put(home.toURI().relativize(file.toURI()).getPath(), checksum(file));
			}
		}
		return checksums;
	}

	//remove files that aren't referenced by any notebook
	private static final Pattern pattern = Pattern.compile("((?:vid|pic|aud|doc)_\\d+\\.)(?:jpe?g|png|tif|gif|3gp|mp4|pdf|docx?|pptx?|xlsx?)");
	private static void filter(Map<String, Long> checksums, File home) {
		Set<String> found = new HashSet<String>();
		for (File folder : home.listFiles()) {
			if (folder.isDirectory()) {
				for (File txt : Notebooks.get(folder).txtdir.listFiles()) {
					Matcher m = pattern.matcher(Util.read(txt));
					while (m.find()) {
						found.add(m.group(1));
					}
				}
			}
		}
		for (Iterator<String> i = checksums.keySet().iterator(); i.hasNext();) {
			String name = new File(i.next()).getName();
			Matcher m = pattern.matcher(name);
			if (m.matches() && !found.contains(m.group(1))) {
				i.remove();
			}
		}
	}

	private static boolean diff(Map<String, Long> checksums, String notebook, Uri serverUri, String username, String password) throws IOException {
		int deleted = 0, unchanged = 0;

		Builder builder = serverUri.buildUpon().appendPath("checksums");
		if (notebook != null) {
			builder.appendQueryParameter("notebook", notebook);
		}

		Object files = Util.json(builder.build().toString(), username, password);
		if (!(files instanceof JSONArray)) {
			return false;
		}
		//iterate through remote files
		for (Object object : (JSONArray) files) {
			JSONObject file = (JSONObject) object;
			String path = (String) file.get("filename");
			if (!checksums.containsKey(path)) {
				//file exists on server but not client
				checksums.put(path, null);
				deleted++;
			} else if (checksums.get(path).equals(file.get("checksum"))) {
				//file exists on server and client and is up-to-date
				checksums.remove(path);
				unchanged++;
			}
		}

		System.out.printf("~~~to be uploaded: %s, to be deleted: %s, unchanged: %s%n", checksums.size() - deleted, deleted, unchanged);

		return true;
	}

	private static boolean sync(Map<String, Long> checksums, Uri serverUri, String username, String password, File home, boolean commit) throws IOException {
		boolean ok = true;
		Uri uri = serverUri.buildUpon().appendPath("file").build();
		for (String path : checksums.keySet()) {
			HttpURLConnection connection;
			if (checksums.get(path) == null) {
				connection = (HttpURLConnection) new URL(uri.buildUpon().appendQueryParameter("path", path).build().toString()).openConnection();
				Util.auth(connection, username, password);
				connection.setRequestMethod("DELETE");
			} else {
				connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
				Util.auth(connection, username, password);
				String boundary = UUID.randomUUID().toString();
				connection.setRequestProperty("Content-Type", String.format("multipart/form-data; boundary=%s", boundary));
				connection.setDoOutput(true);
				write(connection, "--%s", boundary);
				write(connection, "Content-Disposition: form-data; name=\"path\"");
				write(connection, "");
				write(connection, "%s", path);
				write(connection, "--%s", boundary);
				write(connection, "Content-Disposition: form-data; name=\"file\"; filename=\"%s\"", new File(path).getName());
				write(connection, "Content-Type: application/octet-stream");
				write(connection, "");
				write(connection, new File(home, path));
				write(connection, "");
				write(connection, "--%s--", boundary);
			}
			int responseCode = connection.getResponseCode();
			ok &= (HttpURLConnection.HTTP_OK == responseCode);
			System.out.println("~~~" + responseCode + " " + new File(path).getName());
		}

		return ok && complete(serverUri, username, password, commit);
	}

	private static boolean complete(Uri serverUri, String username, String password, boolean commit) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(serverUri.buildUpon().appendPath("complete").appendQueryParameter("commit", String.valueOf(commit)).build().toString()).openConnection();
		Util.auth(connection, username, password);
		return HttpURLConnection.HTTP_OK == connection.getResponseCode();
	}

	private static void write(URLConnection connection, String format, Object...args) throws IOException {
		connection.getOutputStream().write(String.format(format + "\r\n", args).getBytes());
	}

	private static void write(URLConnection connection, File file) throws IOException {
		Util.copy(new FileInputStream(file), connection.getOutputStream(), false);
	}

	private static long checksum(File file) throws IOException {
		CheckedInputStream cis =  new CheckedInputStream(new FileInputStream(file), new CRC32());
		for (byte[] buffer = new byte[4096]; cis.read(buffer) != -1;);
		Checksum checksum = cis.getChecksum();
		cis.close();
		return checksum.getValue();
	}

}
