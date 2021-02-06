/**
 * 
 */
package unknow.server.http.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.FilterChain;

import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.nio.util.Buffers;

/**
 * the calculated path tree to FilterChains
 * 
 * @author unknow
 */
public class PathTree {
	private final byte[] part;
	private final PathTree[] nexts;
	private final EndNode[] ends;
	private final FilterChain[] exact;
	private final FilterChain[] def;

	/**
	 * create a new PathTree
	 * 
	 * @param part  the path part
	 * @param nexts the root node
	 * @param ends  the end with pattern "*.jsp" pattern
	 * @param exact the "" pattern chains
	 * @param def   the "/*" pattern
	 */
	public PathTree(byte[] part, PathTree[] nexts, EndNode[] ends, FilterChain[] exact, FilterChain[] def) {
		this.part = part;
		this.nexts = nexts;
		this.ends = ends;
		this.exact = exact;
		this.def = def;
	}

	public FilterChain[] find(ServletRequestImpl req) {
		List<Buffers> path = req.rawPath();
		byte[] tmp = new byte[1024];

		PathTree last = this;
		int i = 0;
		do {
			Buffers b = path.get(i++);
			tmp = b.toBytes(tmp);
			PathTree n = last.next(tmp, b.size());
			if (n == null)
				break;
			if (i == path.size())
				return n.exact == null ? n.def : n.exact;
			last = n;
		} while (last.nexts != null);

		EndNode[] end = last.ends == null ? ends : last.ends;
		if (end != null) {
			Buffers buffers = path.get(path.size() - 1);
			tmp = buffers.toBytes(tmp);
			for (i = 0; i < end.length; i++) {
				if (endWith(tmp, buffers.size(), end[i].ext))
					return end[i].chain;
			}
		}
		return last.def == null ? def : last.def;
	}

	private final PathTree next(byte[] path, int l) {
		PathTree[] n = nexts;
		for (int i = 0; i < n.length; i++) {
			PathTree node = n[i];
			if (equals(node.part, path, l))
				return node;
		}
		return null;
	}

	private static final boolean equals(byte[] path, byte[] b, int l) {
		if (path.length != l)
			return false;
		for (int i = 0; i < l; i++) {
			if (path[i] != b[i])
				return false;
		}
		return true;
	}

	private static boolean endWith(byte[] b, int l, byte[] end) {
		if (l < end.length)
			return false;
		int o = l - end.length;
		for (int i = 0; i < end.length; i++) {
			if (b[o + i] != end[i])
				return false;
		}
		return true;
	}

	public static final class EndNode {
		private final byte[] ext; // .jsp
		private final FilterChain[] chain;

		/**
		 * create new EndNode
		 * 
		 * @param ext
		 * @param chain
		 */
		public EndNode(byte[] ext, FilterChain[] chain) {
			this.ext = ext;
			this.chain = chain;
		}
	}

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
