/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;

import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlElem;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;

/**
 * @author unknow
 */
public class JaxSaxHandlerBuilderTest {

	@Test
	public void test() {
		XmlObject xml = new XmlObject(this.getClass().getName(), null, Arrays.asList(new XmlField(new XmlType.XmlInt(), "getI", "setI", "i", "")), Arrays.asList(), new XmlElem(new XmlType.XmlString(), "getValue", "setValue"));

		CompilationUnit cu = new CompilationUnit();
		TypeCache types = new TypeCache(cu, new HashMap<>());
		NodeList<BodyDeclaration<?>> build = JaxSaxHandlerBuilder.build(types, xml, t -> null);
		System.out.println(build);
	}

}
