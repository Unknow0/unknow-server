/**
 * 
 */
package unknow.server.http.jaxrs.header;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.MediaType;

/**
 * @author unknow
 */
public class MediaTypeDelegateTest {
	public static final Stream<Arguments> test() {
		Map<String, String> a = new HashMap<>();
		a.put("a", "a");
		a.put("q", "0.5");
		return Stream.of(Arguments.of(new MediaType("text", "xml"), "text/xml", 0, 8),
				Arguments.of(new MediaType("text", "xml", Collections.singletonMap("q", "1")), "text/xml;q=1", 0, 12),
				Arguments.of(new MediaType("text", "xml", Collections.singletonMap("q", "1")), "text/xml;q=1,text/plain;a=a;q=0.5", 0, 12),
				Arguments.of(new MediaType("text", "plain", a), "text/xml;q=1,text/plain;a=a;q=0.5", 13, 33), Arguments.of(null, " *; q=.2", 0, 8));
	}

	@ParameterizedTest
	@MethodSource
	void test(MediaType expected, String value, int off, int end) {
		MediaType m = MediaTypeDelegate.fromString(value, off, end);
		assertEquals(expected, m);
	}
}
