///**
// * 
// */
//package unknow.server.maven.jaxws;
//
//import java.util.Arrays;
//import java.util.HashMap;
//
//import com.github.javaparser.ast.CompilationUnit;
//import com.github.javaparser.ast.NodeList;
//import com.github.javaparser.ast.body.BodyDeclaration;
//
//import unknow.server.maven.TypeCache;
//import unknow.server.maven.jaxws.model.XmlMappingObject;
//import unknow.server.maven.jaxws.model.XmlMappingObject.XmlElem;
//import unknow.server.maven.jaxws.model.XmlMappingObject.XmlField;
//import unknow.server.maven.jaxws.model.XmlMapping;
//
///**
// * @author unknow
// */
//public class JaxSaxHandlerBuilderTest {
//
//	public void test() {
//		XmlMappingObject xml = new XmlMappingObject("", "", this.getClass().getName(), null, Arrays.asList(new XmlField(XmlMapping.XmlInt, "getI", "setI", "i", "")), Arrays.asList(), new XmlElem(XmlMapping.XmlString, "getValue", "setValue"));
//
//		CompilationUnit cu = new CompilationUnit();
//		TypeCache types = new TypeCache(cu, new HashMap<>());
//		NodeList<BodyDeclaration<?>> build = JaxSaxHandlerBuilder.build(types, xml, t -> null);
//		System.out.println(build);
//	}
//
//}
