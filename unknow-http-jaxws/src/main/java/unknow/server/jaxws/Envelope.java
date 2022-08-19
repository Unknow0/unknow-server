package unknow.server.jaxws;

import java.util.ArrayList;
import java.util.List;

public final class Envelope {

	private final List<Object> header = new ArrayList<>();

	private final List<Object> body = new ArrayList<>();

	public void addHeader(Object o) {
		header.add(o);
	}

	public Object getHeader(int i) {
		return header.get(i);
	}

	public int getHeaderSize() {
		return header.size();
	}

	public void addBody(Object o) {
		body.add(o);
	}

	public Object getBody(int i) {
		return body.get(i);
	}

	public int getBodySize() {
		return body.size();
	}

	@Override
	public String toString() {
		return "Envelope [sig=" + sig() + "header=" + header + ", body=" + body + "]";
	}

	public String sig() {
		if (body.size() == 1 & body.get(0) instanceof OperationWrapper)
			return ((OperationWrapper) body.get(0)).getQName();
		StringBuilder sb = new StringBuilder();
		for (Object o : header)
			Envelope.name(sb, o.getClass());
		sb.append('#');
		for (Object o : body)
			Envelope.name(sb, o.getClass());
		return sb.toString();
	}

	private static void name(StringBuilder sb, Class<?> cl) {
		if (cl.isArray())
			sb.append('[').append(cl.getComponentType().getCanonicalName());
		else
			sb.append(cl.getCanonicalName());
		sb.append(';');
	}
}
