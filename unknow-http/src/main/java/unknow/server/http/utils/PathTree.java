/**
 * 
 */
package unknow.server.http.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.FilterChain;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.nio.util.Buffers;

/**
 * the calculated path tree to FilterChains
 * 
 * @author unknow
 */
public class PathTree {
	final PartNode root;

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
	 * @throws InterruptedException
	 */
	public FilterChain find(ServletRequestImpl req) throws InterruptedException {
		req.setPathInfo(1);
		FilterChain f = tryFind(req, root, req.getPaths(), 0);
		return f == null ? root.def : f;
	}

	private FilterChain tryFind(ServletRequestImpl req, PartNode last, List<String> part, int i) throws InterruptedException {
		if (i == part.size()) {
			req.setPathInfo(part.size());
			return last.exact;
		}

		while (last.nexts != null) {
			PartNode n = next(last.nexts, part.get(i));
			if (n == null)
				break;
			if (i + 1 == part.size()) {
				req.setPathInfo(i);
				return n.exact;
			}
			last = n;
			i++;
		}
		if (last.pattern != null) {
			if (i + 1 < part.size()) {
				FilterChain f = tryFind(req, last.pattern, part, i + 1);
				if (f != null) // contextPath already set
					return f;
			} else {
				req.setPathInfo(i);
				return last.pattern.exact;
			}
		}

		if (last.ends != null) {
			Node n = ends(last.ends, part.get(part.size() - 1));
			if (n != null) {
				req.setPathInfo(part.size());
				return n.exact;
			}
		}
		req.setPathInfo(i);
		return last.def;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		root.toString(sb, new StringBuilder());
		return sb.toString();
	}

	private static final PartNode next(PartNode[] nexts, String path) {
		int low = 0;
		int high = nexts.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			PartNode n = nexts[mid];
			int c = compare(n.part, path);
			if (c < 0)
				low = mid + 1;
			else if (c > 0)
				high = mid - 1;
			else
				return n; // key found
		}
		return null;
	}

	private static final Node ends(Node[] nexts, String path) {
		int low = 0;
		int high = nexts.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			Node n = nexts[mid];
			int c = compare(n.part, path, n.part.length());
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
	 * @throws InterruptedException
	 */
	public static int compare(byte[] a, Buffers b, int o, int e) throws InterruptedException {
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
	 * compare two part
	 * 
	 * @param a one
	 * @param b two
	 * @return -1,1 or 0 if a <,> or = to b
	 */
	public static int compare(String a, String b) {
		return compare(a, b, -1);
	}

	/**
	 * compare two part
	 * 
	 * @param a   one
	 * @param b   two
	 * @param max max number of char to compare
	 * @return -1,1 or 0 if a <,> or = to b
	 */
	public static int compare(String a, String b, int max) {
		if (max == 0)
			return 0;
		int ai = a.length();
		int bi = b.length();
		while (bi > 0 && ai > 0) {
			int c = a.charAt(--ai) - b.charAt(--bi);
			if (c != 0)
				return c;
			if (max > 0 && --max == 0)
				return 0;
		}
		return a.length() - b.length();
	}

	/**
	 * node for ending
	 * 
	 * @author unknow
	 */
	public static class Node {
		final String part;
		final FilterChain exact;

		/**
		 * create new EndNode
		 * 
		 * @param part
		 * @param chain
		 */
		public Node(String part, FilterChain chain) {
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
		public PartNode(String part, PartNode[] nexts, PartNode pattern, Node[] ends, FilterChain exact, FilterChain def) {
			super(part, exact);
			this.nexts = nexts;
			this.pattern = pattern;
			this.ends = ends;
			this.def = def;
		}

		public void toString(StringBuilder sb, StringBuilder name) {
			int l = name.length();
			sb.append(l == 0 ? "/" : name).append('\t').append(exact).append('\n');
			for (int i = 0; i < nexts.length; i++) {
				PartNode n = nexts[i];
				name.append('/').append(n.part);
				n.toString(sb, name);
				name.setLength(l);
			}
			if (pattern != null) {
				name.append("/{}");
				pattern.toString(sb, name);
				name.setLength(l);
			}
			name.append("/*");
			sb.append(name).append('\t').append(def).append('\n');
			for (int i = 0; i < ends.length; i++)
				sb.append(name).append(ends[i].part).append('\t').append(ends[i].exact).append('\n');
			name.setLength(l);
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
