/**
 * 
 */
package unknow.server.http.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
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
				Arguments.of(MediaType.WILDCARD_TYPE, "*/*", (MTPredicate) m -> null),
				Arguments.of(MediaType.WILDCARD_TYPE, "*/*", MTPredicate.ANY),
				Arguments.of(MediaType.TEXT_XML_TYPE, "text/plain,text/xml", new MTPredicate.OneOf(MediaType.TEXT_XML_TYPE)),
				Arguments.of(MediaType.WILDCARD_TYPE, null, new MTPredicate.OneOf(MediaType.TEXT_XML_TYPE)),
				Arguments.of(MediaType.TEXT_XML_TYPE, "*/*",new MTPredicate.OneOf(MediaType.TEXT_XML_TYPE)),
				Arguments.of(new MediaType("text", "json"), "text/xml;q=.5,text/json", MTPredicate.ANY));
		//@formatter:on
	}

	@ParameterizedTest(name = "accept {1}")
	@MethodSource
	void accept(MediaType expected, String accept, MTPredicate allowed) {
		RuntimeDelegate.setInstance(new JaxrsRuntime());
		HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
		Mockito.when(r.getHeader("accept")).thenReturn(accept);
		JaxrsReq req = new JaxrsReq(r, Collections.emptyList());
		assertEquals(expected, req.getAccepted(allowed, MediaType.WILDCARD_TYPE));
	}
}