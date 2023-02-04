/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * @author unknow
 */
public class ResponseHeader implements MultivaluedMap<String, Object> {

	private final HttpServletResponse res;

	public ResponseHeader(HttpServletResponse res) {
		this.res = res;
	}

	@Override
	public int size() {
		return res.getHeaderNames().size();
	}

	@Override
	public boolean isEmpty() {
		return res.getHeaderNames().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return res.containsHeader(String.valueOf(key));
	}

	@Override
	public boolean containsValue(Object value) {
		for (String k : res.getHeaderNames()) {
			if (res.getHeaders(k).contains(value))
				return true;
		}
		return false;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Object> get(Object key) {
		Collection<String> headers = res.getHeaders(String.valueOf(key));
		return headers == null ? null : headers instanceof List ? (List) headers : new ArrayList<>(headers);
	}

	@Override
	public List<Object> put(String key, List<Object> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Object> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<Object>> m) {
		for (Entry<? extends String, ? extends List<Object>> e : m.entrySet()) {
			String k = e.getKey();
			for (Object v : e.getValue())
				add(k, v);
		}
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<List<Object>> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<String, List<Object>>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putSingle(String key, Object value) {
		res.setHeader(key, value == null ? null : value.toString());
	}

	@Override
	public void add(String key, Object value) {
		res.addHeader(key, value == null ? null : value.toString());
	}

	@Override
	public String getFirst(String key) {
		return res.getHeader(key);
	}

	@Override
	public void addAll(String key, Object... newValues) {
		for (int i = 0; i < newValues.length; i++)
			res.addHeader(key, newValues[i] == null ? null : newValues[i].toString());
	}

	@Override
	public void addAll(String key, List<Object> valueList) {
		for (Object value : valueList)
			res.addHeader(key, value == null ? null : value.toString());

	}

	@Override
	public void addFirst(String key, Object value) {
		res.addHeader(key, value == null ? null : value.toString());
	}

	@Override
	public boolean equalsIgnoreValueOrder(MultivaluedMap<String, Object> otherMap) {
		// TODO Auto-generated method stub
		return false;
	}
}
