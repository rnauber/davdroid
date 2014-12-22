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
import ch.boye.httpclientandroidlib.conn.DnsResolver;
import java.net.InetAddress;


public class DummyHostNameResolver implements DnsResolver {
	private final static String TAG = "davdroid.DummyHostNameResolver";
	

public InetAddress[] resolve (String host){
		Log.d(TAG, "Dummy-resolving " + host);
		byte[] ip = new byte[]{1, 1, 1, 1};
		InetAddress[] ips= new InetAddress[] {InetAddress.getByAddress(ip)};
		return ips;
}
}
	
