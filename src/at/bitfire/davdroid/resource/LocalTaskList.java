/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.Getter;

import org.apache.commons.lang.StringUtils;
import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class LocalTaskList extends LocalCollection<Task> {
	private static final String TAG = "davdroid.LocalTaskList";

	@Getter protected long id;
	@Getter protected String url;
	
	protected static String COLLECTION_COLUMN_CTAG = TaskLists.SYNC1;

	
	/* database fields */
	
	@Override
	protected Uri entriesURI() {
		return syncAdapterURI(Tasks.CONTENT_URI);
	}

	protected String entryColumnAccountType()	{ return Tasks.ACCOUNT_TYPE; }
	protected String entryColumnAccountName()	{ return Tasks.ACCOUNT_NAME; }
	
	protected String entryColumnParentID()		{ return Tasks.LIST_ID; }
	protected String entryColumnID()			{ return Tasks._ID; }
	protected String entryColumnRemoteName()	{ return Tasks._SYNC_ID; }
	protected String entryColumnETag()			{ return Tasks.SYNC1; }

	protected String entryColumnDirty()			{ return Tasks._DIRTY; }
	protected String entryColumnDeleted()		{ return Tasks._DELETED; }
	protected String entryColumnUID()			{ return Tasks._UID; }

	
	/* class methods, constructor */

	@SuppressLint("InlinedApi")
	public static void create(Account account, ContentResolver resolver, ServerInfo.ResourceInfo info) throws LocalStorageException {
		ContentProviderClient client = resolver.acquireContentProviderClient(TaskContract.AUTHORITY);
		if (client == null)
			throw new LocalStorageException("No task content provider found (Mirakel not installed?)");
		
		int color = 0xFFC3EA6E;		// fallback: "DAVdroid green"
		if (info.getColor() != null) {
			Pattern p = Pattern.compile("#(\\p{XDigit}{6})(\\p{XDigit}{2})?");
			Matcher m = p.matcher(info.getColor());
			if (m.find()) {
				int color_rgb = Integer.parseInt(m.group(1), 16);
				int color_alpha = m.group(2) != null ? (Integer.parseInt(m.group(2), 16) & 0xFF) : 0xFF;
				color = (color_alpha << 24) | color_rgb;
			}
		}
		
		ContentValues values = new ContentValues();
		values.put(TaskLists.ACCOUNT_NAME, account.name);
		values.put(TaskLists.ACCOUNT_TYPE, account.type);
		values.put(TaskLists.SYNC_ENABLED, 1);
		values.put(TaskLists._SYNC_ID, info.getURL());
		values.put(TaskLists.LIST_NAME, info.getTitle());
		values.put(TaskLists.LIST_COLOR, color);
		values.put(TaskLists.OWNER, account.name);
		values.put(TaskLists.VISIBLE, 1);
		
		if (info.isReadOnly())
			Log.w(TAG, "CalDAV to-do list is read-only; changing it locally will result in errors");
		
		Log.i(TAG, "Inserting Mirakel task list: " + values.toString() + " -> " + listsURI(account).toString());
		try {
			client.insert(listsURI(account), values);
		} catch(RemoteException e) {
			throw new LocalStorageException(e);
		}
	}
	
	public static LocalTaskList[] findAll(Account account, ContentProviderClient providerClient) throws RemoteException {
		@Cleanup Cursor cursor = providerClient.query(listsURI(account),
				new String[] { TaskLists._ID, TaskLists._SYNC_ID },
				TaskLists.SYNC_ENABLED + "=1", null, null);
		
		LinkedList<LocalTaskList> taskLists = new LinkedList<LocalTaskList>();
		while (cursor != null && cursor.moveToNext())
			taskLists.add(new LocalTaskList(account, providerClient, cursor.getInt(0), cursor.getString(1)));
		return taskLists.toArray(new LocalTaskList[0]);
	}

	public LocalTaskList(Account account, ContentProviderClient providerClient, long id, String url) throws RemoteException {
		super(account, providerClient);
		this.id = id;
		this.url = url;
	}

	
	/* collection operations */
	
	@Override
	public String getCTag() throws LocalStorageException {
		try {
			@Cleanup Cursor c = providerClient.query(ContentUris.withAppendedId(taskListsURI(), id),
					new String[] { COLLECTION_COLUMN_CTAG }, null, null, null);
			if (c.moveToFirst()) {
				return c.getString(0);
			} else
				throw new LocalStorageException("Couldn't query task list CTag");
		} catch(RemoteException e) {
			throw new LocalStorageException(e);
		}
	}
	
	@Override
	public void setCTag(String cTag) throws LocalStorageException {
		ContentValues values = new ContentValues(1);
		values.put(COLLECTION_COLUMN_CTAG, cTag);
		try {
			providerClient.update(ContentUris.withAppendedId(taskListsURI(), id), values, null, null);
		} catch(RemoteException e) {
			throw new LocalStorageException(e);
		}
	}


	/* create/update/delete */
	
	@Override
	public Task newResource(long localID, String resourceName, String eTag) {
		return new Task(localID, resourceName, eTag);
	}
	
	@Override
	public void deleteAllExceptRemoteNames(Resource[] remoteResources) {
		String where;
		
		if (remoteResources.length != 0) {
			List<String> sqlFileNames = new LinkedList<String>();
			for (Resource res : remoteResources)
				sqlFileNames.add(DatabaseUtils.sqlEscapeString(res.getName()));
			where = entryColumnRemoteName() + " NOT IN (" + StringUtils.join(sqlFileNames, ",") + ")";
		} else
			where = entryColumnRemoteName() + " IS NOT NULL";
		
		Builder builder = ContentProviderOperation.newDelete(entriesURI())
				.withSelection(entryColumnParentID() + "=? AND (" + where + ")", new String[] { String.valueOf(id) });
		pendingOperations.add(builder
				.withYieldAllowed(true)
				.build());
	}
	
	
	/* methods for populating the data object from the content provider */
	

	@Override
	public void populate(Resource resource) throws LocalStorageException {
		Task t = (Task)resource;
		
		try {
			@Cleanup Cursor cursor = providerClient.query(ContentUris.withAppendedId(entriesURI(), t.getLocalID()),
				new String[] {
					/*  0 */ entryColumnUID(), Tasks.TITLE, Tasks.DESCRIPTION
			}, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				t.setUid(cursor.getString(0));
				t.setTitle(cursor.getString(1));
				t.setDescription(cursor.getString(2));
			}
		} catch(SQLiteException ex) {
			throw new LocalStorageException(ex);
		} catch(RemoteException ex) {
			throw new LocalStorageException(ex);
		}
	}
	
	
	/* content builder methods */

	@Override
	protected Builder buildEntry(Builder builder, Resource resource) {
		Task task = (Task)resource;
		builder = builder
			.withValue(entryColumnParentID(), id)
			.withValue(entryColumnRemoteName(), task.getName())
			.withValue(entryColumnETag(), task.getETag())
			.withValue(entryColumnUID(), task.getUid())
			.withValue(Tasks.TITLE, task.getTitle())
			.withValue(Tasks.DESCRIPTION, task.getDescription())
			.withValue(Tasks.STATUS, Tasks.STATUS_DEFAULT);
		return builder;
	}

	
	@Override
	protected void addDataRows(Resource resource, long localID, int backrefIdx) {
	}
	
	@Override
	protected void removeDataRows(Resource resource) {
	}
	
	
	/* private helper methods */
	
	protected static Uri listsURI(Account account) {
		return TaskLists.CONTENT_URI.buildUpon()
				.appendQueryParameter(TaskLists.ACCOUNT_NAME, account.name)
				.appendQueryParameter(TaskLists.ACCOUNT_TYPE, account.type)
				.appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, "true").build();
	}

	protected Uri taskListsURI() {
		return listsURI(account);
	}
}
