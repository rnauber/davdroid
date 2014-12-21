
package at.bitfire.davdroid.webdav;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.OperatedClientConnection;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.SocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.impl.conn.DefaultClientConnectionOperator;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.commons.lang.StringUtils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.socket.LayeredConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.BrowserCompatHostnameVerifier;
import ch.boye.httpclientandroidlib.protocol.HttpContext;
import ch.boye.httpclientandroidlib.conn.socket.ConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.socket.PlainConnectionSocketFactory;


public class PlainSocketFactory extends PlainConnectionSocketFactory {

	private static final String TAG = "davdroid.PlainSocketFactory";
	final static PlainSocketFactory INSTANCE = new PlainSocketFactory();

	private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
	private static final int READ_TIMEOUT_MILLISECONDS = 60000;

	private String mProxyHost="127.0.0.1";
	private int mProxyPort=9050;

	@Override
	public Socket connectSocket(int timeout, Socket plain, HttpHost host, InetSocketAddress remoteAddr, InetSocketAddress localAddr, HttpContext context) throws IOException {
		Log.d(TAG, "Preparing  palin connection with socks proxy " + host);
		
     // Perform explicit SOCKS4a connection request. SOCKS4a supports remote host name resolution
            // (i.e., Tor resolves the hostname, which may be an onion address).
            // The Android (Apache Harmony) Socket class appears to support only SOCKS4 and throws an
            // exception on an address created using INetAddress.createUnresolved() -- so the typical
            // technique for using Java SOCKS4a/5 doesn't appear to work on Android:
            // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/net/PlainSocketImpl.java
            // See also: http://www.mit.edu/~foley/TinFoil/src/tinfoil/TorLib.java, for a similar implementation

            // From http://en.wikipedia.org/wiki/SOCKS#SOCKS4a:
            //
            // field 1: SOCKS version number, 1 byte, must be 0x04 for this version
            // field 2: command code, 1 byte:
            //     0x01 = establish a TCP/IP stream connection
            //     0x02 = establish a TCP/IP port binding
            // field 3: network byte order port number, 2 bytes
            // field 4: deliberate invalid IP address, 4 bytes, first three must be 0x00 and the last one must not be 0x00
            // field 5: the user ID string, variable length, terminated with a null (0x00)
            // field 6: the domain name of the host we want to contact, variable length, terminated with a null (0x00)


            Socket socket = new Socket();
            socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
            socket.connect(new InetSocketAddress(mProxyHost, mProxyPort), CONNECT_TIMEOUT_MILLISECONDS);

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write((byte)0x04);
            outputStream.write((byte)0x01);
            outputStream.writeShort((short)host.getPort());
            outputStream.writeInt(0x01);
            outputStream.write((byte)0x00);
            outputStream.write(host.getHostName()());
            outputStream.write((byte)0x00);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            if (inputStream.readByte() != (byte)0x00 || inputStream.readByte() != (byte)0x5a) {
                throw new IOException("SOCKS4a connect failed");
            }
            inputStream.readShort();
            inputStream.readInt();

return socket;

}


}
