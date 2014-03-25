package uk.ac.ic.bss.labbook.backend.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class DummyContentProvider extends ContentProvider {

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		System.out.println("@@@DummyContentProvider.delete");
		return 0;
	}

	@Override
	public String getType(Uri arg0) {
		System.out.println("@@@DummyContentProvider.getType");
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		System.out.println("@@@DummyContentProvider.insert");
		return null;
	}

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
			String arg4) {
		System.out.println("@@@DummyContentProvider.query");
		return null;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		System.out.println("@@@DummyContentProvider.update");
		return 0;
	}

}
