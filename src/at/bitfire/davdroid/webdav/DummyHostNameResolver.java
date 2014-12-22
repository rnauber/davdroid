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
import ch.boye.httpclientandroidlib.conn.scheme.HostNameResolver;
import java.net.InetAddress;


public class DummyHostNameResolver implements HostNameResolver {
	private final static String TAG = "davdroid.DavHttpClient";
	

public InetAddress resolve (String hostname){
		Log.d(TAG, "Dummy-resolving " + hostname);
		return InetAddress.getByName("1.1.1.1");
}
}
	
