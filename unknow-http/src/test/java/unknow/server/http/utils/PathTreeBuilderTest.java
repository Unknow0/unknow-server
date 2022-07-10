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
		PartNode tree = new PathTreeBuilder(new ServletConfigImpl[0], new FilterConfigImpl[0], DispatcherType.REQUEST).build().root;
		assertEquals(0, tree.nexts.length);
		assertNull(tree.pattern);
		assertEquals(0, tree.ends.length);
		assertNotNull(tree.def);
		assertNotNull(tree.exact);
		assertEquals("ServletDefault", tree.exact.toString());
		assertEquals("ServletDefault", tree.def.toString());
	}

	@Test
	public void testEnd() {
		Set<DispatcherType> dispatcher = new HashSet<>(Arrays.asList(DispatcherType.REQUEST));
		Set<String> urls = new HashSet<>(Arrays.asList("*.html"));
		FilterConfigImpl[] filters = { new FilterConfigImpl(null, new F("f"), null, null, Collections.emptySet(), urls, dispatcher) };

		PartNode tree = new PathTreeBuilder(new ServletConfigImpl[0], filters, DispatcherType.REQUEST).build().root;
		assertEquals(0, tree.nexts.length);
		assertNull(tree.pattern);
		assertEquals(1, tree.ends.length);
		assertEquals("f,ServletDefault", tree.ends[0].exact.toString());
		assertNotNull(tree.def);
		assertNotNull(tree.exact);
		assertEquals("ServletDefault", tree.exact.toString());
		assertEquals("ServletDefault", tree.def.toString());
	}
}
