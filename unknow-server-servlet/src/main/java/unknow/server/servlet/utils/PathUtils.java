/**
 * 
 */
package unknow.server.servlet.utils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author unknow
 */
public class PathUtils {
	private static final byte[] HEX = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private PathUtils() {
	}

	/**
	 * % encode
	 * @param s the string
	 * @return the encoded bytes
	 */
	public static byte[] encodePart(String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		int l = bytes.length;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] < 0 || bytes[i] == 20)
				l += 2;
		}
		if (l == bytes.length)
			return bytes;
		byte[] b = new byte[l];
		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			byte c = bytes[i];
			if (c < 0 || c == 20) {
				b[j++] = '%';
				b[j++] = HEX[c / 16];
				b[j++] = HEX[c % 16];
			} else
				b[j++] = c;
		}
		return b;
	}

	public static void pathQuery(Reader r, Map<String, List<String>> p) throws IOException {
		int c;
		String key;
		StringBuilder sb = new StringBuilder();
		do {
			c = readParam(sb, r, true);
			key = sb.toString();
			sb.setLength(0);
			if (c == '=')
				c = readParam(sb, r, false);

			p.computeIfAbsent(key, k -> new ArrayList<>(1)).add(sb.toString());
			sb.setLength(0);
		} while (c != -1);
	}

	private static int readParam(StringBuilder sb, Reader r, boolean key) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '&' || key && c == '=')
				return c;
			if (c == '%')
				c = decodeHex(r.read()) << 8 | decodeHex(r.read());
			sb.append((char) c);
		}
		return c;
	}

	private static int decodeHex(int c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'A' && c <= 'F')
			return 10 + c - 'A';
		if (c >= 'a' && c <= 'f')
			return 10 + c - 'A';
		return 0;
	}
}
