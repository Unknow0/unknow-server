/**
 * 
 */
package unknow.server.http.utils;

import unknow.server.nio.util.Buffers;
import unknow.server.nio.util.BuffersUtils;

/**
 * @author unknow
 */
public interface PathMatcher {

	int length();

	boolean match(Buffers path);

	public static class ExactMatcher implements PathMatcher {
		private final byte[] match;

		@Override
		public int length() {
			return match.length;
		}

		public ExactMatcher(byte[] match) {
			this.match = match;
		}

		@Override
		public boolean match(Buffers path) {
			return BuffersUtils.equals(path, match);
		}
	}

	public static class StartMatcher implements PathMatcher {
		private final byte[] match;

		public StartMatcher(byte[] match) {
			this.match = match;
		}

		@Override
		public int length() {
			return match.length;
		}

		@Override
		public boolean match(Buffers path) {
			return BuffersUtils.pathMatches(path, match);
		}
	}

	public static class EndMatcher implements PathMatcher {
		private final byte[] match;

		@Override
		public int length() {
			return -1;
		}

		public EndMatcher(byte[] match) {
			this.match = match;
		}

		@Override
		public boolean match(Buffers path) {
			return BuffersUtils.endsWith(path, match);
		}
	}

	public static class AndMatcher implements PathMatcher {
		private final PathMatcher a;
		private final PathMatcher b;

		public AndMatcher(PathMatcher a, PathMatcher b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int length() {
			return a.length();
		}

		@Override
		public boolean match(Buffers path) {
			return a.match(path) && b.match(path);
		}
	}
}
