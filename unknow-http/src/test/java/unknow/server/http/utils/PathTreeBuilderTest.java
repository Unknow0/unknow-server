/**
 * 
 */
package unknow.server.http.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;

import org.junit.jupiter.api.Test;

import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.utils.PathTree.PartNode;

/**
 * @author unknow
 */
public class PathTreeBuilderTest {
	@Test
	public void testDefault() {
		PathTree build = new PathTreeBuilder(new ServletConfigImpl[0], new FilterConfigImpl[0], DispatcherType.REQUEST).build();
		System.out.println("default:\n" + build);
		PartNode tree = build.root;
		assertNode(tree, "ServletDefault", "ServletDefault");
		assertEquals(0, tree.nexts.length);
		assertNull(tree.pattern);
		assertEquals(0, tree.ends.length);
	}

	@Test
	public void testEnd() {
		Set<DispatcherType> dispatcher = new HashSet<>(Arrays.asList(DispatcherType.REQUEST));
		Set<String> urls = new HashSet<>(Arrays.asList("*.html"));
		FilterConfigImpl[] filters = { new FilterConfigImpl(null, new F("f"), null, null, Collections.emptySet(), urls, dispatcher) };

		PathTree build = new PathTreeBuilder(new ServletConfigImpl[0], filters, DispatcherType.REQUEST).build();
		System.out.println("end:\n" + build);
		PartNode tree = build.root;
		assertNode(tree, "ServletDefault", "ServletDefault");
		assertEquals(0, tree.nexts.length);
		assertNull(tree.pattern);
		assertEquals(1, tree.ends.length);
		assertEquals("f,ServletDefault", tree.ends[0].exact.toString());
	}

	@Test
	public void testServletExact() {
		Set<String> urls = new HashSet<>(Arrays.asList("/test", ""));
		ServletConfigImpl[] servlets = { new ServletConfigImpl("name", new S("s"), null, null, urls) };
		PathTree build = new PathTreeBuilder(servlets, new FilterConfigImpl[0], DispatcherType.REQUEST).build();
		System.out.println("exact:\n" + build);
		PartNode tree = build.root;
		assertNode(tree, "s", "ServletDefault");
		assertEquals(1, tree.nexts.length);
		assertNode(tree.nexts[0], "s", "ServletDefault");
		assertNull(tree.pattern);
		assertEquals(0, tree.ends.length);
	}

	@Test
	public void testServletDefault() {
		Set<String> urls = new HashSet<>(Arrays.asList("/test/*"));
		ServletConfigImpl[] servlets = { new ServletConfigImpl("name", new S("s"), null, null, urls) };
		PathTree build = new PathTreeBuilder(servlets, new FilterConfigImpl[0], DispatcherType.REQUEST).build();
		System.out.println("exact:\n" + build);
		PartNode tree = build.root;
		assertNode(tree, "ServletDefault", "ServletDefault");
		assertEquals(1, tree.nexts.length);
		assertNode(tree.nexts[0], "s", "s");
		assertNull(tree.pattern);
		assertEquals(0, tree.ends.length);
	}

	private static void assertNode(PartNode n, String exact, String def) {
		assertNotNull(n.exact);
		assertEquals(exact, n.exact.toString());
		assertNotNull(n.def);
		assertEquals(def, n.def.toString());

	}
}
