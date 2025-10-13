package unknow.server.maven.jaxb;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import unknow.model.api.ModelLoader;
import unknow.model.api.TypeModel;
import unknow.server.maven.jaxb.model.XmlType;

public class HandlerContext {
	private final Map<XmlType, String> handlers;
	private final ModelLoader loader;
	private final XmlType xml;

	private final Map<TypeModel, String> emptyArray;

	public HandlerContext(Map<XmlType, String> handlers, ModelLoader loader, XmlType xml) {
		this.handlers = handlers;
		this.loader = loader;
		this.xml = xml;

		this.emptyArray = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public <T extends XmlType> T xml() {
		return (T) xml;
	}

	public TypeModel type() {
		return xml.type();
	}

	public String handler(XmlType t) {
		return handlers.get(t);
	}

	public TypeModel type(String name) {
		return loader.get(name);
	}

	public void emptyArray(TypeModel type, Consumer<String> c) {
		emptyArray.computeIfAbsent(type, k -> {
			String name = "EMPTY$" + emptyArray.size();
			c.accept(name);
			return name;
		});
	}

	public String emptyArray(TypeModel type) {
		return emptyArray.get(type);
	}
}
