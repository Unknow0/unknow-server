/**
 * 
 */
package unknow.server.http.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.utils.PathTree.Node;
import unknow.server.http.utils.PathTree.PartNode;

/**
 * @author unknow
 */
public class PathTreeTest {
	List<String> path;
	ServletRequestImpl mock;

	@BeforeEach
	public void init() {
		path = new ArrayList<>();
		mock = mock(ServletRequestImpl.class, Mockito.withSettings().useConstructor(null, null, DispatcherType.REQUEST, null));
		Mockito.when(mock.getPaths()).thenReturn(path);
	}

	@Test
	public void root() throws InterruptedException {
		FilterChain exacts = new FC("exacts");
		FilterChain defaults = new FC("defaults");

		PathTree tree = new PathTree(new PartNode(null, null, null, exacts, defaults));

		assertEquals(exacts, tree.find(mock));

		path.add("blabla");
		assertEquals(defaults, tree.find(mock));
	}

	@Test
	public void nexts() throws InterruptedException {
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
	public void ends() throws InterruptedException {
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