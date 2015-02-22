/*******************************************************************************
 * Copyright (c) 2014
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.webdav;

import android.util.Log;
import at.bitfire.davdroid.Constants;
import org.apache.http.conn.DnsResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class DummyHostNameResolver implements DnsResolver {
	private final static String TAG = "davdroid.DummyHostNameResolver";


	public InetAddress[] resolve (String host){
		InetAddress ip = null;
		InetAddress[] ips = {};

		if (host.endsWith(".onion")) {
			Log.d(TAG, "Dummy-resolving " + host);
			byte[] ipbytes = new byte[]{1, 1, 1, 1}; //invalid address
			try {
				ip = InetAddress.getByAddress(ipbytes);
				ips = new InetAddress[] {ip};
			}
			catch (UnknownHostException e) {
				Log.d(TAG, "WTF?" + e);
			}
		}
		else
		{
			try {
				ips = InetAddress.getAllByName(host);
			}
			catch (UnknownHostException e) {
				Log.d(TAG, "Could not resolve " + host);
			}
		}

		return ips;
	}
}
