package unknow.server.maven.jaxws.binding;

import java.util.List;

import javax.xml.namespace.QName;

public class Operation {
	public final String m;
	public final QName name;
	public final boolean wrapped;
	public final String action;
	public final List<Parameter> params;
	public final Parameter result;

	public Operation(String m, QName name, boolean wrapped, String action, List<Parameter> params, Parameter result) {
		this.m = m;
		this.name = name;
		this.wrapped = wrapped;
		this.action = action;
		this.params = params;
		this.result = result;
	}

	public String sig() {
		if (wrapped)
			return action + "/" + name;
		StringBuilder sb = new StringBuilder(action).append("/");
		for (Parameter o : params) {
			if (o.header)
				sb.append(o.type.name()).append(';');
		}
		sb.append('#');
		for (Parameter o : params) {
			if (!o.header)
				sb.append(o.type.name()).append(';');
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Operation: " + name;
	}
}