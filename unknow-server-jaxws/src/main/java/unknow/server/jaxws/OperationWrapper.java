package unknow.server.jaxws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OperationWrapper {
	private final String ns;
	private final String name;
	private final String qname;
	private final List<Object> values;

	public OperationWrapper(String ns, String name, Object... values) {
		this.ns = ns;
		this.name = name;
		this.qname = ns == null ? name : '{' + ns + '}' + name;
		this.values = Arrays.asList(values);
	}

	public OperationWrapper(String qname) {
		if (qname.charAt(0) == '{') {
			int i = qname.indexOf('}');
			this.ns = qname.substring(0, i);
			this.name = qname.substring(i + 1);
		} else {
			this.ns = null;
			this.name = qname;
		}
		this.qname = qname;
		this.values = new ArrayList<>();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the ns
	 */
	public String getNs() {
		return ns;
	}

	/**
	 * @return the qname
	 */
	public String getQName() {
		return qname;
	}

	/**
	 * @return the values
	 */
	public List<Object> getValues() {
		return values;
	}

	/**
	 * @param i
	 * @return i'th value
	 */
	public Object get(int i) {
		return values.get(i);
	}

	/**
	 * @param o object to add
	 */
	public void add(Object o) {
		values.add(o);
	}

	@Override
	public String toString() {
		return "OperationWrapper [ns=" + ns + ", name=" + name + ", values=" + values + "]";
	}

}
