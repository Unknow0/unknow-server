/**
 * 
 */
package unknow.server.maven.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * @author unknow
 */
public class JaxRsServletTest {
	@Test
	public void testMimeOrder() {
		ArrayList<String> list = new ArrayList<>(Arrays.asList("a/b", "a/*", "*/b", "*/*"));
		list.sort(JaxRsServlet.MIME);
		assertEquals(Arrays.asList("*/*", "*/b", "a/*", "a/b"), list);
	}
}
