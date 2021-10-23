///**
// * 
// */
//package unknow.server.maven.jaxws;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNull;
//
//import javax.xml.bind.annotation.XmlAccessType;
//import javax.xml.bind.annotation.XmlAccessorType;
//import javax.xml.bind.annotation.XmlAttribute;
//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;
//import javax.xml.bind.annotation.XmlValue;
//
//import org.junit.Test;
//
//import com.github.javaparser.JavaParser;
//import com.github.javaparser.ParserConfiguration;
//import com.github.javaparser.ast.CompilationUnit;
//import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
//import com.github.javaparser.symbolsolver.JavaSymbolSolver;
//import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
//
//import unknow.server.maven.jaxws.XmlMetadata.XmlElem;
//import unknow.server.maven.jaxws.XmlMetadata.XmlField;
//
///**
// * @author unknow
// */
//public class XmlMetadataTest {
//	@XmlRootElement(name = "A", namespace = "ns1")
//	@XmlAccessorType(XmlAccessType.FIELD)
//	public static class A {
//		@XmlValue
//		private int value;
//		@XmlAttribute(name = "attr")
//		private int i;
//		@XmlElement(name = "elem1")
//		private String v;
//		@XmlElement(name = "elem2", namespace = "ns2")
//		private String v2;
//	}
//
//	private static CompilationUnit parse(String s) {
//		JavaParser parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
//		return parser.parse(s).getResult().get();
//	}
//
//	@Test(expected = RuntimeException.class)
//	public void testMissingGetter() {
//		CompilationUnit cu = parse("package test;\n"
//				+ "import javax.xml.bind.annotation.XmlAccessorType;\n"
//				+ "import javax.xml.bind.annotation.XmlRootElement;\n"
//				+ "@XmlRootElement(name = \"A\", namespace = \"ns1\")\n"
//				+ "@XmlAccessorType(XmlAccessType.FIELD)\n"
//				+ "public class A {\n"
//				+ "	private String v;\n"
//				+ "}");
//
//		XmlMetadata.loadFrom(cu.findFirst(ClassOrInterfaceDeclaration.class).get(), "ns1");
//	}
//
//	@Test(expected = RuntimeException.class)
//	public void testDuplicateValue() {
//		CompilationUnit cu = parse("package test;\n"
//				+ "import javax.xml.bind.annotation.XmlAccessorType;\n"
//				+ "import javax.xml.bind.annotation.XmlRootElement;\n"
//				+ "@XmlRootElement(name = \"A\", namespace = \"ns1\")\n"
//				+ "@XmlAccessorType(XmlAccessType.FIELD)\n"
//				+ "public class A {\n"
//				+ "	@XmlValue\n"
//				+ "	private String v;\n"
//				+ "	@XmlValue\n"
//				+ "	private String v2;\n"
//				+ "	public void setV(String v) { this.v=v;}\n"
//				+ "	public String getV() { return v;}\n"
//				+ "	public void setV2(String v) { this.v=v;}\n"
//				+ "	public String getV2() { return v;}\n"
//				+ "}");
//
//		XmlMetadata.loadFrom(cu.findFirst(ClassOrInterfaceDeclaration.class).get(), "ns1");
//	}
//
//	@Test
//	public void testFieldValue() {
//		CompilationUnit cu = parse("package test;\n"
//				+ "import javax.xml.bind.annotation.XmlAccessorType;\n"
//				+ "import javax.xml.bind.annotation.XmlRootElement;\n"
//				+ "@XmlRootElement(name = \"A\", namespace = \"ns1\")\n"
//				+ "@XmlAccessorType(XmlAccessType.FIELD)\n"
//				+ "public class A {\n"
//				+ "	@XmlValue\n"
//				+ "	private String v;\n"
//				+ "	public void setV(String v) { this.v=v;}\n"
//				+ "	public String getV() { return v;}\n"
//				+ "}");
//
//		XmlMetadata m = XmlMetadata.loadFrom(cu.findFirst(ClassOrInterfaceDeclaration.class).get(), "ns1");
//		assertEquals(0, m.elems.size());
//		assertEquals(0, m.attrs.size());
//		assertEquals(new XmlElem("java.lang.String", "getV", "setV"), m.value);
//	}
//
//	@Test
//	public void testFieldAttribute() {
//		CompilationUnit cu = parse("package test;\n"
//				+ "import javax.xml.bind.annotation.*;\n"
//				+ "@XmlRootElement(name = \"A\", namespace = \"ns1\")\n"
//				+ "@XmlAccessorType(XmlAccessType.FIELD)\n"
//				+ "public class A {\n"
//				+ "	@XmlAttribute\n"
//				+ "	private String v;\n"
//				+ "	public void setV(String v) { this.v=v;}\n"
//				+ "	public String getV() { return v;}\n"
//				+ "}");
//
//		XmlMetadata m = XmlMetadata.loadFrom(cu.findFirst(ClassOrInterfaceDeclaration.class).get(), "ns1");
//		assertEquals(0, m.elems.size());
//		assertNull(m.value);
//		assertEquals(1, m.attrs.size());
//		assertEquals(new XmlField("java.lang.String", "getV", "setV", "v", ""), m.attrs.get(0));
//	}
//
//	@Test
//	public void testFieldElem() {
//		CompilationUnit cu = parse("package test;\n"
//				+ "import javax.xml.bind.annotation.*;\n"
//				+ "@XmlRootElement(name = \"A\", namespace = \"ns1\")\n"
//				+ "@XmlAccessorType(XmlAccessType.FIELD)\n"
//				+ "public class A {\n"
//				+ "	@XmlElement\n"
//				+ "	private String v;\n"
//				+ "	public void setV(String v) { this.v=v;}\n"
//				+ "	public String getV() { return v;}\n"
//				+ "}");
//
//		XmlMetadata m = XmlMetadata.loadFrom(cu.findFirst(ClassOrInterfaceDeclaration.class).get(), "ns1");
//		assertEquals(0, m.attrs.size());
//		assertNull(m.value);
//		assertEquals(1, m.elems.size());
//		assertEquals(new XmlField("java.lang.String", "getV", "setV", "v", "ns1"), m.elems.get(0));
//	}
//}
