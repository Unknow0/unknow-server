/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author unknow
 */
public class XMLNsCollector implements XMLWriter {
	private final Map<String, Integer> ns = new HashMap<>();

	@Override
	public void close() throws IOException {
	}

	@Override
	public void startElement(String name, String nsUri) throws IOException {
		ns.merge(nsUri, 1, Integer::sum);
	}

	@Override
	public void attribute(String name, String nsUri, String value) throws IOException {
		ns.merge(nsUri, 1, Integer::sum);
	}

	@Override
	public void text(String text) throws IOException {
	}

	@Override
	public void endElement(String name, String nsUri) throws IOException {
		ns.merge(nsUri, 1, Integer::sum);
	}

	public Map<String, String> buildNsMapping() {
		if (ns.isEmpty())
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<>();
		List<String> list = new ArrayList<>(ns.keySet());
		Collections.sort(list, (a, b) -> ns.get(a) - ns.get(b));
		Iterator<String> it = list.iterator();
		map.put(it.next(), "");
		int i = 0;
		while (it.hasNext())
			map.put(it.next(), prefix(i++));
		return map;
	}

	private static final char[] PREFIX_FIRST = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b', 'n', '_', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N' };
	private static final char[] PREFIX_OTHER = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b', 'n', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.' };

	/**
	 * generate a xmlns prefix
	 * 
	 * @param t the value to encode
	 * @return the prefix
	 */
	public static String prefix(int t) {
		if (t < PREFIX_FIRST.length)
			return new String(PREFIX_FIRST, t, 1);
		StringBuilder sb = new StringBuilder();
		int i = t % PREFIX_FIRST.length;
		sb.append(PREFIX_FIRST[i]);
		t -= i;
		while (t > PREFIX_OTHER.length) {
			i = t % PREFIX_OTHER.length;
			sb.append(PREFIX_OTHER[i]);
			t -= i;
		}
		sb.append(PREFIX_OTHER[t]);
		return sb.toString();
	}

}
