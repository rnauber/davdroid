package at.bitfire.davdroid.syncadapter;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import at.bitfire.davdroid.resource.CalDavTodoList;
import at.bitfire.davdroid.resource.LocalCollection;
import at.bitfire.davdroid.resource.LocalTaskList;
import at.bitfire.davdroid.resource.RemoteCollection;

public class TasksSyncAdapterService extends Service {
	private static SyncAdapter syncAdapter;
	
	@Override
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new SyncAdapter(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		syncAdapter.close();
		syncAdapter = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder(); 
	}
	

	private static class SyncAdapter extends DavSyncAdapter {
		private final static String TAG = "davdroid.TasksSyncAdapter";

		
		private SyncAdapter(Context context) {
			super(context);
		}
		
		@Override
		protected Map<LocalCollection<?>, RemoteCollection<?>> getSyncPairs(Account account, ContentProviderClient provider) {
			AccountSettings settings = new AccountSettings(getContext(), account);
			String	userName = settings.getUserName(),
					password = settings.getPassword();
			boolean preemptive = settings.getPreemptiveAuth();

			try {
				Map<LocalCollection<?>, RemoteCollection<?>> map = new HashMap<LocalCollection<?>, RemoteCollection<?>>();
				
				for (LocalTaskList taskList : LocalTaskList.findAll(account, provider)) {
					RemoteCollection<?> dav = new CalDavTodoList(httpClient, taskList.getUrl(), userName, password, preemptive);
					map.put(taskList, dav);
				}
				return map;
			} catch (RemoteException ex) {
				Log.e(TAG, "Couldn't find local task lists", ex);
			} catch (URISyntaxException ex) {
				Log.e(TAG, "Couldn't build task lists URI", ex);
			}
			
			return null;
		}
	}
}
