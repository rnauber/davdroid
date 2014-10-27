package at.bitfire.davdroid.resource.test;

import lombok.Cleanup;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.test.InstrumentationTestCase;
import android.util.Log;

public class LocalTaskListTest extends InstrumentationTestCase {
	private static final String
		TAG = "davdroid.LocalTaskListTest",
		taskListName = "DAVdroid_TestTasks";
	
	ContentProviderClient providerClient;
	Account testAccount = new Account(taskListName, TaskContract.LOCAL_ACCOUNT);
	long taskListId;
	
	
	// helpers
	
	private Uri syncAdapterURI(Uri uri) {
		return uri.buildUpon()
				.appendQueryParameter(TaskLists.ACCOUNT_NAME, taskListName)
				.appendQueryParameter(TaskLists.ACCOUNT_TYPE, TaskContract.LOCAL_ACCOUNT)
				.appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, "true").
				build();
	}

	
	// initialization

	protected void setUp() throws Exception {
		ContentResolver resolver = getInstrumentation().getContext().getContentResolver();
		providerClient = resolver.acquireContentProviderClient(TaskContract.AUTHORITY);
		assertNotNull(providerClient);

		@Cleanup Cursor cursor = providerClient.query(TaskLists.CONTENT_URI,
				new String[] { TaskLists._ID },
				TaskLists.ACCOUNT_TYPE + "=? AND " + TaskLists.LIST_NAME + "=?",
				new String[] { CalendarContract.ACCOUNT_TYPE_LOCAL, taskListName },
				null);
		if (cursor.moveToNext()) {
			// found local test task list
			taskListId = cursor.getLong(0);
			Log.d(TAG, "Found test task list with ID " + taskListId);
		} else {
			// no local test task list found, create 
			ContentValues values = new ContentValues();
			values.put(TaskLists.ACCOUNT_NAME, testAccount.name);
			values.put(TaskLists.ACCOUNT_TYPE, testAccount.type);
			values.put(TaskLists.SYNC_ENABLED, 1);
			values.put(TaskLists.LIST_NAME, taskListName);
			values.put(TaskLists.VISIBLE, 1);
			
			Uri taskListURI = providerClient.insert(syncAdapterURI(TaskLists.CONTENT_URI), values);
			
			taskListId = ContentUris.parseId(taskListURI);
			Log.d(TAG, "Created test task list with ID " + taskListId);
		}
	}
	
	protected void tearDown() throws Exception {
		Log.d(TAG, "Deleting test task list " + taskListId);
		Uri uri = ContentUris.withAppendedId(syncAdapterURI(TaskLists.CONTENT_URI), taskListId);
		providerClient.delete(uri, null, null);
	}

	
	// tests
	
	public void testNull() {
		assert(true);
	}
}
