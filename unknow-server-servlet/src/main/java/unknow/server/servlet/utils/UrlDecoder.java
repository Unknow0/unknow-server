package unknow.server.servlet.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UrlDecoder {
	private static final byte EQ = '=';
	private static final byte AM = '&';
	private static final byte SP = ' ';
	private static final int[] HEX = new int[256];

	static {
		Arrays.fill(HEX, -1);
		for (int i = '0'; i <= '9'; i++)
			HEX[i] = i - '0';
		for (int i = 'A'; i <= 'F'; i++)
			HEX[i] = 10 + i - 'A';
		for (int i = 'a'; i <= 'f'; i++)
			HEX[i] = 10 + i - 'a';
	}

	private final InputStream in;
	private final byte[] buf;
	private int off;
	private int len;

	private byte[] cbuf;
	private int coff;

	private int pctState; // 0 normal, 1 = %, 2 = %X
	private int pctHi;

	private String key;

	public UrlDecoder(InputStream in) {
		this.in = in;
		this.buf = new byte[4096];
		this.cbuf = new byte[4096];
	}

	public void process(Map<String, List<String>> map) throws IOException {
		while ((len = in.read(buf)) != -1) {
			off = 0;
			parse(map);
		}
		if (coff > 0) {
			if (key == null)
				key = string();
			add(map, string());
		}
	}

	private void parse(Map<String, List<String>> map) {
		while (off < len) {
			if (key == null) {
				key = decode();
				if (key == null)
					continue;
				if (buf[off - 1] == AM) {
					add(map, "");
					continue;
				}
			}

			String v = decode();
			if (v != null)
				add(map, v);

		}
	}

	private void add(Map<String, List<String>> map, String value) {
		List<String> list = map.get(key);
		if (list == null)
			map.put(key, list = new ArrayList<>(1));
		list.add(value);
		key = null;
	}

	private String decode() {
		int s = off;
		while (off < len) {
			byte c = buf[off++];
			if (pctState == 0 && c != '%' && c != '+' && c != EQ && c != AM)
				continue;

			append(s);
			if (pctState == 1) {
				pctHi = HEX[c & 0xFF];
				if (pctHi < 0)
					throw new IllegalArgumentException();
				pctState = 2;
				continue;
			}
			if (pctState == 2) {
				int lo = HEX[c & 0xFF];
				if (lo < 0)
					throw new IllegalArgumentException();
				int val = (pctHi << 4) | lo;
				append((byte) val);
				pctState = 0;
				continue;
			}
			if (c == EQ || c == AM)
				return string();

			if (c == '%') {
				pctState = 1;
				continue;
			}
			if (c == '+')
				append(SP);
		}
		append(s);
		return null;

	}

	private String string() {
		if (coff == 0)
			return "";
		String s = new String(cbuf, 0, coff, StandardCharsets.UTF_8);
		coff = 0;
		return s;
	}

	private void capacity(int l) {
		while (cbuf.length < l)
			cbuf = Arrays.copyOf(cbuf, cbuf.length * 2);
	}

	private void append(int s) {
		int l = off - s;
		if (l == 0)
			return;
		capacity(coff + l);
		System.arraycopy(buf, s, cbuf, coff, l);
		coff += l;
	}

	private void append(byte c) {
		capacity(coff + 1);
		cbuf[coff++] = c;
	}
}
