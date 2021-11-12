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
	private final byte[] part;
	private final PathTree[] nexts;
	// TODO add pattern
	private final EndNode[] ends;
	private final FilterChain exact;
	private final FilterChain def;

	/**
	 * create a new PathTree
	 * 
	 * @param part  the path part
	 * @param nexts the root node
	 * @param ends  the end with pattern "*.jsp" pattern
	 * @param exact the "" pattern chains
	 * @param def   the "/*" pattern
	 */
	public PathTree(byte[] part, PathTree[] nexts, EndNode[] ends, FilterChain exact, FilterChain def) {
		this.part = part;
		this.nexts = nexts;
		this.ends = ends;
		this.exact = exact;
		this.def = def;
	}

	public FilterChain find(HttpHandler req) {
		Buffers path = req.meta;
		int o = req.pathStart() + 1;
		int e = req.pathEnd();
		if (o == e)
			return exact == null ? def : exact;

		PathTree last = this;
		while (last.nexts != null) {
			int i = path.indexOf((byte) '/', o, e - o);
			if (i < 0)
				i = e;
			PathTree n = last.next(path, o, i);
			if (n == null)
				break;
			if (i == e)
				return n.exact == null ? n.def : n.exact;
			last=n;
			o = i + 1;
		}

		EndNode[] end = last.ends == null ? ends : last.ends;
		if (end != null) {
			for (;;) {
				int i = path.indexOf((byte) '/', o, e - o);
				if (i < 0)
					break;
				o = i + 1;
			}
			int l = e - o;
			for (int i = 0; i < end.length; i++) {
				byte[] ext = end[i].ext;
				if (ext.length < l && path.equals(ext, e - ext.length))
					return end[i].chain;
			}
		}
		// TODO
//		req.setPathInfoStart(o);
		return last.def == null ? def : last.def;
	}

	private final PathTree next(Buffers path, int o, int e) {
		PathTree[] n = nexts;
		int l = e - o;
		for (int i = 0; i < n.length; i++) {
			PathTree node = n[i];
			if (node.part.length == l && path.equals(node.part, o))
				return node;
		}
		return null;
	}

	public static final class EndNode {
		private final byte[] ext; // .jsp
		private final FilterChain chain;

		/**
		 * create new EndNode
		 * 
		 * @param ext
		 * @param chain
		 */
		public EndNode(byte[] ext, FilterChain chain) {
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
