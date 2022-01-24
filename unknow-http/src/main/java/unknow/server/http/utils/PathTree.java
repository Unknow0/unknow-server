/**
 * 
 */
package unknow.server.http.utils;

import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;

import unknow.server.http.HttpHandler;
import unknow.server.nio.util.Buffers;

/**
 * the calculated path tree to FilterChains
 * 
 * @author unknow
 */
public class PathTree {
	private final PartNode root;

	/**
	 * create a new PathTree
	 * 
	 * @param root root part
	 */
	public PathTree(PartNode root) {
		this.root = root;
	}

	/**
	 * find the chain to call
	 * 
	 * @param req the request
	 * @return the chain
	 */
	public FilterChain find(HttpHandler req) {
		req.setPathInfoStart(1);
		FilterChain f = tryFind(req, root, req.meta, req.pathStart() + 1, req.pathEnd());
		return f == null ? root.def : f;
	}

	private FilterChain tryFind(HttpHandler req, PartNode last, Buffers path, int o, int e) {
		if (o == e) {
			req.setPathInfoStart(e);
			return last.exact;
		}

		while (last.nexts != null) {
			int i = path.indexOf((byte) '/', o, e - o);
			if (i < 0)
				i = e;
			PartNode n = next(last.nexts, path, o, i);
			if (n == null)
				break;
			if (i == e) {
				req.setPathInfoStart(e);
				return n.exact;
			}
			last = n;
			o = i + 1;
		}
		if (last.pattern != null) {
			int i = path.indexOf((byte) '/', o, e - o);
			if (i > 0) {
				FilterChain f = tryFind(req, last.pattern, path, i + 1, e);
				if (f != null) // contextPath already set
					return f;
			} else {
				req.setPathInfoStart(e);
				return last.pattern.exact;
			}
		}

		if (last.ends != null) {
			Node n = ends(last.ends, path, o, e);
			if (n != null) {
				req.setPathInfoStart(e);
				return n.exact;
			}
		}
		req.setPathInfoStart(o);
		return last.def;
	}

	private static final PartNode next(PartNode[] nexts, Buffers path, int o, int e) {
		int low = 0;
		int high = nexts.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			PartNode n = nexts[mid];
			int c = compare(n.part, path, o, e);
			if (c < 0)
				low = mid + 1;
			else if (c > 0)
				high = mid - 1;
			else
				return n; // key found
		}
		return null;
	}

	private static final Node ends(Node[] nexts, Buffers path, int o, int e) {
		int low = 0;
		int high = nexts.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			Node n = nexts[mid];
			int c = compare(n.part, path, Math.max(o, e - n.part.length), e);
			if (c < 0)
				low = mid + 1;
			else if (c > 0)
				high = mid - 1;
			else
				return n; // key found
		}
		return null;
	}

	/**
	 * compare two part
	 * 
	 * @param a one
	 * @param b two
	 * @return -1,1 or 0 if a <,> or = to b
	 */
	public static int compare(byte[] a, byte[] b) {
		int ai = a.length;
		int bi = b.length;
		while (ai >= 0 && bi >= 0) {
			int c = a[--ai] - b[--bi];
			if (c != 0)
				return c;
		}
		return a.length - b.length;
	}

	/**
	 * compare two part
	 * 
	 * @param a one
	 * @param b two
	 * @param o start of the part in the buffer
	 * @param e end of the part
	 * @return -1,1 or 0 if a <,> or = to b
	 */
	public static int compare(byte[] a, Buffers b, int o, int e) {
		int i = a.length;
		int bl = e - o;
		while (o < e && i >= 0) {
			int c = a[--i] - b.get(--e);
			if (c != 0)
				return c;
		}
		return a.length - bl;
	}

	/**
	 * node for ending
	 * 
	 * @author unknow
	 */
	public static class Node {
		final byte[] part; // .jsp
		final FilterChain exact;

		/**
		 * create new EndNode
		 * 
		 * @param part
		 * @param chain
		 */
		public Node(byte[] part, FilterChain chain) {
			this.part = part;
			this.exact = chain;
		}
	}

	public static class PartNode extends Node {
		final PartNode[] nexts;
		final PartNode pattern;
		final Node[] ends;
		final FilterChain def;

		/**
		 * create a new PartNode
		 * 
		 * @param part    the path part
		 * @param nexts   the child part
		 * @param pattern the pattern child (try if the child match)
		 * @param ends    the end with pattern "*.jsp" pattern
		 * @param exact   the "" pattern chains
		 * @param def     the "/*" pattern
		 */
		public PartNode(byte[] part, PartNode[] nexts, PartNode pattern, Node[] ends, FilterChain exact, FilterChain def) {
			super(part, exact);
			this.nexts = nexts;
			this.pattern = pattern;
			this.ends = ends;
			this.def = def;
		}
	}

	/**
	 * url encode a path part
	 * 
	 * @param s part to encode
	 * @return encoded part
	 */
	public static byte[] encodePart(String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		int l = bytes.length;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] < 0 || bytes[i] == 20)
				l += 2;
		}
		if (l == bytes.length)
			return bytes;
		byte[] b = new byte[l];
		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			byte c = bytes[i];
			if (c < 0 || c == 20) {
				b[j++] = '%';
				b[j++] = HEX[c / 16];
				b[j++] = HEX[c % 16];
			} else
				b[j++] = c;
		}
		return b;
	}

	private static final byte[] HEX = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
}
