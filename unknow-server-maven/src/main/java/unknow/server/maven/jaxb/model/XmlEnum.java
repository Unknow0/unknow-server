/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.bind.annotation.XmlEnumValue;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.EnumModel.EnumConstant;

/**
 * @author unknow
 */
public class XmlEnum extends XmlTypeSimple {
	private final XmlTypeSimple base;
	private final List<XmlEnumEntry> entries;

	public XmlEnum(QName qname, EnumModel type, XmlTypeSimple base) {
		super(qname, type);
		this.base = base;
		this.entries = new ArrayList<>();
		for (EnumConstant e : type.entries())
			entries.add(new XmlEnumEntry(e.name(), e.annotation(XmlEnumValue.class).flatMap(a -> a.value()).filter(v -> v.isSet()).map(a -> a.asLiteral()).orElse(e.name())));
	}

	public XmlTypeSimple base() {
		return base;
	}

	public List<XmlEnumEntry> entries() {
		return entries;
	}

	@Override
	public String toString() {
		return "Enum" + entries;
	}

	public static final class XmlEnumEntry {
		private final String name;
		private final String value;

		public XmlEnumEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String name() {
			return name;
		}

		public String value() {
			return value;
		}

		@Override
		public String toString() {
			return name + ": " + value;
		}
	}
}