/**
 * 
 */
package unknow.server.maven.sax;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.SAXException;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

import unknow.server.maven.descriptor.Descriptor;
import unknow.server.maven.descriptor.LD;

/**
 * @author unknow
 */
public class Listener extends Handler {

	public Listener(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("listener-class".equals(localName)) {
			ResolvedReferenceTypeDeclaration descl = ctx.resolver.tryToSolveType(ctx.sb.toString()).getCorrespondingDeclaration();
			Set<Class<?>> impl = new HashSet<>();
			for (ResolvedReferenceType a : descl.getAllAncestors()) {
				String n = a.getQualifiedName();
				for (Class<?> c : Descriptor.LISTENERS) {
					if (n.equals(c.getName())) {
						impl.add(c);
						break;
					}
				}
			}
			if (!impl.isEmpty())
				ctx.descriptor.listeners.add(new LD(lastContent(), impl));
		} else if ("listener".equals(localName)) {
			ctx.reader.setContentHandler(previous);
		}
	}
}
