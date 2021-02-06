/**
 * 
 */
package unknow.server.http;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import unknow.server.nio.util.Buffers;

/**
 * represent an http request
 * 
 * @author unknow
 */
public class HttpRawRequest {
	/** the method */
	public final Buffers method = new Buffers();
	/** the full path */
	public final List<Buffers> path = new ArrayList<>();
	/** the query string */
	public final Buffers query = new Buffers();
	/** the protocol */
	public final Buffers protocol = new Buffers();

	/** the headers */
	public final RawHeader headers = new RawHeader();
	RawHeader headerTail = null;

	public InetSocketAddress local;
	public InetSocketAddress remote;

	/** the content */
	Buffers content;

	/** get the next header to write */
	RawHeader nextHeader() {
		// TODO limit number of header
		if (headerTail == null)
			return headerTail = headers;
		if (headerTail.next == null)
			headerTail.next = new RawHeader();
		RawHeader n = headerTail.next;
		headerTail = n;
		return n;
	}

	/**
	 * clear all data
	 */
	public void reset() {
		local = remote = null;
		method.clear();
		path.clear();
		protocol.clear();

		content = null;

		// cleanup headers
		headerTail = null;
		RawHeader h = headers;
		while (h != null) {
			h.clear();
			h.value.clear();
			h = h.next;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("HttpRawRequest:\n");
		sb.append(method).append(" ").append(path).append(" ").append(protocol).append('\n');
		RawHeader h = headers;
		while (h != headerTail && !h.isEmpty()) {
			sb.append(h).append(": ").append(h.value).append('\n');
			h = h.next;
		}
		return sb.toString();
	}

	/**
	 * an http header
	 * 
	 * @author unknow
	 */
	public static class RawHeader extends Buffers {
		/** the value */
		public final Buffers value = new Buffers();
		/** next header in chain */
		public RawHeader next;

		@Override
		public boolean isEmpty() {
			return super.isEmpty() && value.isEmpty();
		}
	}
}