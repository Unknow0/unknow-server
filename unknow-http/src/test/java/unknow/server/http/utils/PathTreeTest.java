/**
 * 
 */
package unknow.server.http.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import unknow.server.http.HttpHandler;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.PathTree.Node;
import unknow.server.http.utils.PathTree.PartNode;

/**
 * @author unknow
 */
public class PathTreeTest {
	HttpHandler mock;

	@BeforeEach
	public void init() {
		ServletContextImpl ctx = mock(ServletContextImpl.class);
		mock = mock(HttpHandler.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS).useConstructor(null, null, ctx, -1));
		Mockito.when(mock.pathStart()).thenReturn(0);
	}

	@Test
	public void root() {
		FilterChain exacts = new F("exacts");
		FilterChain defaults = new F("defaults");

		PathTree tree = new PathTree(new PartNode(null, null, null, null, exacts, defaults));

		mock.meta.clear();
		mock.meta.write('/');
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(exacts, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/blabla".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(defaults, tree.find(mock));
	}

	@Test
	public void nexts() {
		F defaults = new F("defaults");
		F first = new F("first");
		F second = new F("second");
		PartNode[] next = { first.part(), second.part() };
		Arrays.sort(next, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, next, null, null, null, defaults));

		mock.meta.clear();
		mock.meta.write("/toto".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(defaults, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/first".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(first, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/second".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(second, tree.find(mock));
	}

	@Test
	public void ends() {
		F defaults = new F("defaults");
		F jsp = new F(".jsp");
		F html = new F(".html");
		Node[] ends = { jsp.node(), html.node() };
		Arrays.sort(ends, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, null, null, ends, null, defaults));

		mock.meta.clear();
		mock.meta.write("/bla.txt".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(defaults, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/bla.jsp".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(jsp, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/bla.html".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(html, tree.find(mock));
	}

	@Test
	public void pattern() {
		F defaults = new F("defaults");
		F patternExa = new F("pattern exact");
		F patternDef = new F("pattern default");
		F end = new F("end");
		PartNode partNode = new PartNode(null, new PartNode[] { end.part() }, null, null, patternExa, patternDef);
		PathTree tree = new PathTree(new PartNode(null, null, partNode, null, defaults, defaults));

		mock.meta.clear();
		mock.meta.write("/".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(defaults, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/test".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(patternExa, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/test/bla".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(patternDef, tree.find(mock));

		mock.meta.clear();
		mock.meta.write("/test/end".getBytes());
		Mockito.when(mock.pathEnd()).thenReturn(mock.meta.length());
		assertEquals(end, tree.find(mock));
	}

	private static class F implements FilterChain {
		private final String name;

		public F(String name) {
			this.name = name;
		}

		public PartNode part() {
			return new PartNode(name.getBytes(), null, null, null, this, this);
		}

		public Node node() {
			return new Node(name.getBytes(), this);
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		}
	}
}
