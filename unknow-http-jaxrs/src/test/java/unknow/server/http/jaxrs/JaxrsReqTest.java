/**
 * 
 */
package unknow.server.http.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author unknow
 */
public class JaxrsReqTest {

	@ParameterizedTest(name = "path {1}")
	@MethodSource
	public void path(String expected, String path) {
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		Mockito.when(r.getServletPath()).thenReturn(path);
		JaxrsReq req = new JaxrsReq(r, new JaxrsPath[] { new JaxrsPath(1, "p") });
		assertEquals(expected, req.getPath("p", null, JaxrsContext.STRING));
	}

	public static final Stream<Arguments> path() {
		return Stream.of(
				Arguments.of("path", "/path"),
				Arguments.of("path", "/path/toto"));
	}

	@ParameterizedTest(name = "query {1}")
	@MethodSource
	public void query(String expected, String query, String def) {
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		JaxrsReq req = new JaxrsReq(r, new JaxrsPath[0]);
		Mockito.when(r.getQueryString()).thenReturn(query);
		assertEquals(expected, req.getQuery("n", def, JaxrsContext.STRING));
	}

	public static final Stream<Arguments> query() {
		return Stream.of(
				Arguments.of(null, null, null),
				Arguments.of("d", null, "d"),
				Arguments.of("d", "a=a", "d"),
				Arguments.of("n", "n=n", "d"),
				Arguments.of("n", "a=a&n=n", "d"),
				Arguments.of("n", "a=a&n=n&b=b", "d"),
				Arguments.of("n", "a=a&n=n&n=b", "d"));
	}
}