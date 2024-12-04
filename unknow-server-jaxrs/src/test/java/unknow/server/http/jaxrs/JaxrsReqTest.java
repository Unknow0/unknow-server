/**
 * 
 */
package unknow.server.http.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.impl.DefaultConvert;

/**
 * @author unknow
 */
public class JaxrsReqTest {

	public static final Stream<Arguments> query() {
		return Stream.of(Arguments.of(null, null, null), Arguments.of("d", null, "d"), Arguments.of("d", "a=a", "d"), Arguments.of("n", "n=n", "d"),
				Arguments.of("n", "a=a&n=n", "d"), Arguments.of("n", "a=a&n=n&b=b", "d"), Arguments.of("n", "a=a&n=n&n=b", "d"));
	}

	@ParameterizedTest(name = "query {1}")
	@MethodSource
	public void query(String expected, String query, String def) {
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		Mockito.when(r.getQueryString()).thenReturn(query);
		JaxrsReq req = new JaxrsReq(r, Collections.emptyList());
		assertEquals(expected, req.getQuery("n", def, DefaultConvert.STRING));
	}

	public static final Stream<Arguments> matrix() {
		return Stream.of(Arguments.of(null, "", null), Arguments.of("d", "/test", "d"), Arguments.of("d", "/test;a=a", "d"), Arguments.of("n", "/test;n=n", "d"),
				Arguments.of("n", "/test;a=a;n=n", "d"), Arguments.of("n", "/test;a=a;n=n;b=b", "d"), Arguments.of("n", "/test;a=a;n=n;n=b", "d"));
	}

	@ParameterizedTest(name = "matrix {1}")
	@MethodSource
	void matrix(String expected, String path, String def) {
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		Mockito.when(r.getRequestURI()).thenReturn(path);
		JaxrsReq req = new JaxrsReq(r, Collections.emptyList());
		assertEquals(expected, req.getMatrix("n", def, DefaultConvert.STRING));
	}

	public static final Stream<Arguments> accept() {
		//@formatter:off
		return Stream.of(
				Arguments.of(MediaType.WILDCARD_TYPE, "*/*", (Predicate<MediaType>) m -> false),
				Arguments.of(MediaType.WILDCARD_TYPE, "*/*", (Predicate<MediaType>) m -> true),
				Arguments.of(MediaType.TEXT_XML_TYPE, "text/plain,text/xml", (Predicate<MediaType>) m -> m.getSubtype().equals("xml")),
				Arguments.of(MediaType.WILDCARD_TYPE, null, (Predicate<MediaType>) m -> m.getSubtype().equals("xml")),
				Arguments.of(MediaType.WILDCARD_TYPE, "*/*", (Predicate<MediaType>) m -> m.getSubtype().equals("xml")),
				Arguments.of(new MediaType("text", "json"), "text/xml;q=.5,text/json", (Predicate<MediaType>) m -> true));
		//@formatter:on
	}

	@ParameterizedTest(name = "accept {1}")
	@MethodSource
	void accept(MediaType expected, String accept, Predicate<MediaType> allowed) {
		RuntimeDelegate.setInstance(new JaxrsRuntime());
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		Mockito.when(r.getHeader("accept")).thenReturn(accept);
		JaxrsReq req = new JaxrsReq(r, Collections.emptyList());
		assertEquals(expected, req.getAccepted(allowed, MediaType.WILDCARD_TYPE));
	}
}