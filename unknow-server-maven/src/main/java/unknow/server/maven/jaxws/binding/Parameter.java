package unknow.server.maven.jaxws.binding;

import javax.xml.namespace.QName;

import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.model.TypeModel;

public class Parameter {
	public final QName name;
	public final TypeModel type;
	public final XmlType xml;
	public final boolean header;

	public Parameter(QName name, TypeModel type, XmlType xml, boolean header) {
		this.name = name;
		this.type = type;
		this.xml = xml;
		this.header = header;
	}
}
