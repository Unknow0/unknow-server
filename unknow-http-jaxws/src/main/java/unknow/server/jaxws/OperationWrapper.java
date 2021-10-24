package unknow.server.jaxws;

import java.util.Arrays;

public final class OperationWrapper {
	public final String ns;
	public final String name;
	public final Element[] values;

	public OperationWrapper(String ns, String name, Element... values) {
		this.ns = ns;
		this.name = name;
		this.values = values;
	}

	@Override
	public String toString() {
		return "OperationWrapper [ns=" + ns + ", name=" + name + ", values=" + Arrays.toString(values) + "]";
	}

}
