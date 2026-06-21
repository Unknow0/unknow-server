/**
 * 
 */
package unknow.server.servlet.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.PathTree.Node;
import unknow.server.servlet.utils.PathTree.PartNode;

/**
 * @author unknow
 */
public class PathTreeTest {
	ServletRequestImpl req;

	@BeforeEach
	public void init() {
//		ServletContextImpl ctx = new ServletContextImpl("", "", null, null, null, null, null);

		req = new ServletRequestImpl(null, DispatcherType.REQUEST);
	}

	@Test
	public void root() {
		FilterChain exacts = new FC("exacts");
		FilterChain defaults = new FC("defaults");

		PathTree tree = new PathTree(new PartNode(null, null, null, exacts, defaults));

		req.setRequestUri("/");
		assertEquals(exacts, tree.find(req));

		req.setRequestUri("/blabla");
		assertEquals(defaults, tree.find(req));
	}

	@Test
	public void nexts() {
		FC defaults = new FC("defaults");
		FC first = new FC("first");
		FC second = new FC("second");
		PartNode[] next = { first.part(), second.part() };
		Arrays.sort(next, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, next, null, null, defaults));

		req.setRequestUri("/toto");
		assertEquals(defaults, tree.find(req));

		req.setRequestUri("/first");
		assertEquals(first, tree.find(req));

		req.setRequestUri("/second");
		assertEquals(second, tree.find(req));
	}

	@Test
	public void ends() {
		FC defaults = new FC("defaults");
		FC jsp = new FC(".jsp");
		FC html = new FC(".html");
		Node[] ends = { jsp.node(), html.node() };
		Arrays.sort(ends, (a, b) -> PathTree.compare(a.part, b.part));

		PathTree tree = new PathTree(new PartNode(null, null, ends, null, defaults));

		req.setRequestUri("/bla.txt");
		assertEquals(defaults, tree.find(req));

		req.setRequestUri("/bla.jsp");
		assertEquals(jsp, tree.find(req));

		req.setRequestUri("/bla.html");
		assertEquals(html, tree.find(req));
	}

}
