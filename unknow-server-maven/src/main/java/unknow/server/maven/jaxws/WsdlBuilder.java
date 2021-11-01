/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import unknow.server.maven.jaxws.JaxwsServletBuilder.Op;
import unknow.server.maven.jaxws.JaxwsServletBuilder.Param;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;

/**
 * @author unknow
 */
public class WsdlBuilder {
	public WsdlBuilder() {
	}

	public static void build(Collection<Op> operations) {
		Set<String> schemaNs = new HashSet<>();
		for (Op o : operations) {
			schemaNs.add(o.ns);
			for (Param p : o.params)
				collectNs(p.type, schemaNs);
		}

		// for each ns build schema
	}

	private static void collectNs(XmlType type, Set<String> schemaNs) {
		if (type.isSimple())
			return;
		XmlObject o = (XmlObject) type;
		for (XmlField e : o.attrs) {
			if (!e.ns.isEmpty())
				schemaNs.add(e.ns);
		}
		for (XmlField e : o.elems) {
			schemaNs.add(e.ns);
			collectNs(e.type, schemaNs);
		}
	}
}
