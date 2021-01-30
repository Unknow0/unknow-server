/**
 * 
 */
package unknow.server.http;

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
}
