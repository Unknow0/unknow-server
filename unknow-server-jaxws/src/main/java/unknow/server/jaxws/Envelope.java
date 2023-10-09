package unknow.server.jaxws;

import java.util.ArrayList;
import java.util.List;

public final class Envelope {
	private final List<Object> header;
	private final List<Object> body;

	public Envelope() {
		header = new ArrayList<>(0);
		body = new ArrayList<>(1);
	}

	public Envelope(List<Object> header, List<Object> body) {
		this.header = header;
		this.body = body;
	}

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
		return "Envelope [header=" + header + ", body=" + body + "]";
	}

	public void sig(StringBuilder sb) {
		if (body.size() == 1 && body.get(0) instanceof OperationWrapper) {
			sb.append(((OperationWrapper) body.get(0)).name().toString());
			return;
		}
		for (Object o : header)
			sb.append(o.getClass().getName()).append(';');
		sb.append('#');
		for (Object o : body)
			sb.append(o.getClass().getName()).append(';');
	}
}
