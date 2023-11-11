package unknow.server.jaxws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

public final class OperationWrapper {
	private final QName name;
	private final List<Object> values;

	public OperationWrapper(QName name, Object... values) {
		this.name = name;
		this.values = Arrays.asList(values);
	}

	public OperationWrapper(QName name) {
		this.name = name;
		this.values = new ArrayList<>();
	}

	/**
	 * @return operation name
	 */
	public QName name() {
		return name;

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
		return "OperationWrapper [name=" + name + ", values=" + values + "]";
	}

}
