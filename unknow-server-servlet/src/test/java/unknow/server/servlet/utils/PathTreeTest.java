/**
 * 
 */
package unknow.server.servlet.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import unknow.server.servlet.HttpAdapter;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.PathTree.Node;
import unknow.server.servlet.utils.PathTree.PartNode;

/**
 * @author unknow
 */
public class PathTreeTest {
	List<String> path;
	ServletRequestImpl mock;

	@BeforeEach
	public void init() {
		path = new ArrayList<>();
		ServletContextImpl ctx = new ServletContextImpl("", "", null, null, null, null, null, null);
		HttpAdapter p = mock(HttpAdapter.class);
		when(p.ctx()).thenReturn(ctx);

		mock = mock(ServletRequestImpl.class, Mockito.withSettings().useConstructor(p, DispatcherType.REQUEST));
		when(mock.getPaths()).thenReturn(path);
	}

	@Test
	public void root() {
		FilterChain exacts = new FC("exacts");
		FilterChain defaults = new FC("defaults");

		PathTree tree = new PathTree(new PartNode(null, null, null, exacts, defaults));

		assertEquals(exacts, tree.find(mock));

		path.add("blabla");
		assertEquals(defaults, tree.find(mock));
	}

	@Test
	public void nexts() {
		FC defaults = new FC("defaults");
		FC first = new FC("first");
		FC second = new FC("second");
		PartNode[] next = { first.part(), second.part() };
		Arrays.sort(next, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, next, null, null, defaults));

		path.add("toto");
		assertEquals(defaults, tree.find(mock));

		path.set(0, "first");
		assertEquals(first, tree.find(mock));

		path.set(0, "second");
		assertEquals(second, tree.find(mock));
	}

	@Test
	public void ends() {
		FC defaults = new FC("defaults");
		FC jsp = new FC(".jsp");
		FC html = new FC(".html");
		Node[] ends = { jsp.node(), html.node() };
		Arrays.sort(ends, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, null, ends, null, defaults));

		path.add("bla.txt");
		assertEquals(defaults, tree.find(mock));

		path.set(0, "bla.jsp");
		assertEquals(jsp, tree.find(mock));

		path.set(0, "bla.html");
		assertEquals(html, tree.find(mock));
	}

}
