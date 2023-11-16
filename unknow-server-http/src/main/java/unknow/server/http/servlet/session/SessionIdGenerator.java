/**
 * 
 */
package unknow.server.http.servlet.session;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author unknow
 */
public class SessionIdGenerator {
	private static final char[] ALPHABET = new char[] {
			'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h',
			'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b', 'n', 'A', 'Z', 'E', 'R', 'T', 'Y',
			'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X',
			'C', 'V', 'B', 'N', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', '_'
	};
	private final String nodeId;
	private final AtomicInteger count;

	public SessionIdGenerator(String nodeId) {
		this.nodeId = nodeId;
		this.count = new AtomicInteger(0);
	}

	public String generate() {
		StringBuilder sb = new StringBuilder(nodeId);
		sb.append('-');
		encode(sb, System.currentTimeMillis());
		sb.append('-');
		encode(sb, count.getAndIncrement());
		return sb.toString();
	}

	private static void encode(StringBuilder sb, long value) {
		while (value > 0) {
			sb.append(ALPHABET[(int) (value & 0x3F)]);
			value >>>= 6;
		}
	}
}
