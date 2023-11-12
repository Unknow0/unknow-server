/**
 * 
 */
package unknow.server.http.utils;

import java.nio.charset.StandardCharsets;

/**
 * @author unknow
 */
public class Encoder {
	private static final byte[] HEX = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

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
}
