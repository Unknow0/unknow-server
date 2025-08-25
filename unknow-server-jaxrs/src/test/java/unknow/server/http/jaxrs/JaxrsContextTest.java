/**
 * 
 */
package unknow.server.http.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author unknow
 */
public class JaxrsContextTest {
	String[] s;
	List<String> l;
	List<? extends String> d;
	A a;
	List<String>[] g;

	public static final Stream<Arguments> params() throws NoSuchFieldException, SecurityException {
		Class<?> c = JaxrsContextTest.class;
		return Stream.of( //
				Arguments.of(String.class, c.getDeclaredField("s").getGenericType()), //
				Arguments.of(String.class, c.getDeclaredField("l").getGenericType()), //
				Arguments.of(String.class, c.getDeclaredField("d").getGenericType()), //
				Arguments.of(String.class, c.getDeclaredField("a").getGenericType()), //
				Arguments.of(c.getDeclaredField("l").getGenericType(), c.getDeclaredField("g").getGenericType()));
	}

	@ParameterizedTest
	@MethodSource
	void params(Type expected, Type t) {
		assertEquals(expected, JaxrsContext.getParamType(t));
	}

	public static class A extends ArrayList<String> {
		private static final long serialVersionUID = 1L;
	}
}
