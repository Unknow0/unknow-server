/**
 * 
 */
package unknow.server.http.utils;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

import unknow.server.http.utils.PathTree.EndNode;

/**
 * @author unknow
 */
public class PathTreeTest {
	private static final FilterChain[] C0 = new FilterChain[] { new F("C0") };
	private static final FilterChain[] C1 = new FilterChain[] { new F("C1") };
	private static final FilterChain[] C2 = new FilterChain[] { new F("C2") };
	private static final FilterChain[] C3 = new FilterChain[] { new F("C3") };
	private static final FilterChain[] C4 = new FilterChain[] { new F("C4") };

	private static final byte[] TEST = new byte[] { 't', 'e', 's', 't' };
	private static final byte[] FOO = new byte[] { 'f', 'o', 'o' };
	private static final byte[] BAR = new byte[] { 'b', 'a', 'r' };

	private static final byte[] JSP = new byte[] { '.', 'j', 's', 'p' };

	@Test
	public void test() {
		PathTree p = new PathTree(null, new PathTree[] {
				new PathTree(FOO, null, new EndNode[] { new EndNode(JSP, C2) }, C1, null)
		}, new EndNode[] {
				new EndNode(JSP, C3)
		}, C4, C0);

//		assertArrayEquals("check default", C0, p.find(TEST));
//		assertArrayEquals("check default ending", C3, p.find(TEST, JSP));
//		assertArrayEquals("check exact path", C1, p.find(FOO));
//		assertArrayEquals("check exact path not match", C0, p.find(FOO, TEST));
//		assertArrayEquals("check path ending", C2, p.find(FOO, JSP));
	}

	private static class F implements FilterChain {
		private final String str;

		public F(String str) {
			this.str = str;
		}

		@Override
		public String toString() {
			return str;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		}
	}
}
