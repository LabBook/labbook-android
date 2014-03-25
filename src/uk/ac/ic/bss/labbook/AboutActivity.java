package uk.ac.ic.bss.labbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ic.bss.labbook.backend.RegistrationActivity;
import uk.ac.ic.bss.labbook.backend.Version;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;

public class AboutActivity extends TrackedListActivity {

	private List<Map<String, String>> data = new ArrayList<Map<String, String>>();

	@SuppressWarnings({ "serial" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		data.add(new HashMap<String, String>() {{
			put("text1", "Version");
			put("text2", Version.versionName(AboutActivity.this));
		}});
		data.add(new HashMap<String, String>() {{
			put("text1", "Revision");
			put("text2", String.valueOf(Version.versionCode(AboutActivity.this)));
		}});
		data.add(new HashMap<String, String>() {{
			put("text1", RegistrationActivity.isRegistered() ? "Registered to" : "Unregistered");
			put("text2", RegistrationActivity.isRegistered() ? RegistrationActivity.getEmail() : "");
		}});
		data.add(new HashMap<String, String>() {{
			put("text1", "Terms & Privacy");
			put("href", "https://labbook.cc/application/privacy");
		}});
		data.add(new HashMap<String, String>() {{
			put("text1", "Open-source licences");
			put("href", "https://labbook.cc/application/licence");
		}});
		setListAdapter(new SimpleAdapter(this, data, android.R.layout.two_line_list_item, new String[] {"text1", "text2"}, new int[] {android.R.id.text1, android.R.id.text2}));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (data.get(position).containsKey("href")) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(data.get(position).get("href"))));
		}
	}
}