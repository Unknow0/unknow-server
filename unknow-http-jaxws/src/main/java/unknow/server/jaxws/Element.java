package unknow.server.jaxws;

public final class Element {
	public final String ns;
	public final String name;
	public final Object value;

	public Element(String ns, String name, Object value) {
		this.ns = ns;
		this.name = name;
		this.value = value;
	}
}
