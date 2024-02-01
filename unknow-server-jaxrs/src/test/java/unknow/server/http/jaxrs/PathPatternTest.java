package unknow.server.http.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.http.jaxrs.PathPattern.PathRegexp;
import unknow.server.http.jaxrs.PathPattern.PathSimple;

public class PathPatternTest {

	public static final Stream<Arguments> test() {
		//@formatter:off
		return Stream.of(
				Arguments.of(Arrays.asList("a"), new PathSimple(true), "/a"),
				Arguments.of(null, new PathSimple(true), "/a/b"),
				Arguments.of(null, new PathSimple(false, "t"), "/t/a"),
				Arguments.of(Arrays.asList("a"), new PathSimple(false, "t"), "/a/t"),
				Arguments.of(null, new PathSimple(false, "t"), "/a/ta"),
				Arguments.of(null, new PathSimple(true, "t/"), "/a/t/"),
				Arguments.of(Arrays.asList("a", "b"), new PathSimple(true, "t/"), "/a/t/b"),
				
				Arguments.of(Arrays.asList("t/b"), new PathRegexp("/a/(.*)"), "/a/t/b")
				);
		//@formatter:on
	}

	@ParameterizedTest
	@MethodSource
	public void test(List<String> expected, PathPattern p, String path) {
		assertEquals(expected, p.process(path));
	}

}
