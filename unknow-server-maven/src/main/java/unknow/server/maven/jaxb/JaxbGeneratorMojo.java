/**
 * 
 */
package unknow.server.maven.jaxb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.annotation.XmlRootElement;
import unknow.server.jaxb.ContextFactory;
import unknow.server.jaxb.StrReader;
import unknow.server.jaxb.XmlHandler;
import unknow.server.jaxb.XmlRootHandler;
import unknow.server.jaxb.XmlSimpleHandler;
import unknow.server.jaxb.handler.BigDecimalHandler;
import unknow.server.jaxb.handler.BigIntegerHandler;
import unknow.server.jaxb.handler.BooleanHandler;
import unknow.server.jaxb.handler.ByteHandler;
import unknow.server.jaxb.handler.DoubleHandler;
import unknow.server.jaxb.handler.FloatHandler;
import unknow.server.jaxb.handler.IntHandler;
import unknow.server.jaxb.handler.LongHandler;
import unknow.server.jaxb.handler.ShortHandler;
import unknow.server.jaxb.handler.StringHandler;
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlEnum;
import unknow.server.maven.jaxb.model.XmlLoader;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.jaxb.model.XmlTypeComplex;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxb-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxbGeneratorMojo extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(JaxbGeneratorMojo.class);

	private final Map<XmlType, String> handlers = new HashMap<>();
	private final XmlLoader xmlLoader = new XmlLoader();

	public JaxbGeneratorMojo() {
		handlers.put(XmlLoader.BOOLEAN, BooleanHandler.class.getName());
		handlers.put(XmlLoader.BYTE, ByteHandler.class.getName());
		handlers.put(XmlLoader.SHORT, ShortHandler.class.getName());
//		handlers.put(xmlLoader.add(PrimitiveModel.CHAR), CharHandler.class.getName());
		handlers.put(XmlLoader.INT, IntHandler.class.getName());
		handlers.put(XmlLoader.LONG, LongHandler.class.getName());
		handlers.put(XmlLoader.FLOAT, FloatHandler.class.getName());
		handlers.put(XmlLoader.DOUBLE, DoubleHandler.class.getName());
		handlers.put(XmlLoader.STRING, StringHandler.class.getName());
		handlers.put(XmlLoader.BIGINT, BigIntegerHandler.class.getName());
		handlers.put(XmlLoader.BIGDEC, BigDecimalHandler.class.getName());
	}

	@Override
	protected String id() {
		return "jaxb-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		processSrc();

		for (String c : classes.keySet()) {
			TypeModel type = loader.get(c);
			if (type.annotation(jakarta.xml.bind.annotation.XmlType.class).isPresent())
				xmlLoader.add(type);
		}

		int i = 0;
		for (XmlType t : xmlLoader.types()) {
			if (!"http://www.w3.org/2001/XMLSchema".equals(t.ns()))
				handlers.put(t, packageName + "." + t.type().simpleName() + "_" + i++);
		}
		// default handers

		for (XmlType t : xmlLoader.types()) {
			if ("http://www.w3.org/2001/XMLSchema".equals(t.ns()))
				continue;

			if (t instanceof XmlEnum)
				buildHandler((XmlEnum) t);
			else if (t instanceof XmlTypeComplex)
				buildHandler((XmlTypeComplex) t);
		}

		buildContext();
	}

	private void buildHandler(XmlEnum xml) throws MojoExecutionException {
		TypeModel t = xml.type();
		logger.info("process {}", t);

		// TODO get metadata

		String n = handlers.get(xml);
		int j = n.lastIndexOf('.');
		if (j > 0)
			n = n.substring(j + 1);

		CompilationUnit cu = newCu();
		TypeCache types = new TypeCache(cu, existingClass);
		ClassOrInterfaceDeclaration cl = cu.addClass(n)
				.addImplementedType(types.getClass(XmlSimpleHandler.class, types.get(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, n), "INSTANCE", new ObjectCreationExpr(null, new ClassOrInterfaceType(null, n), Utils.list()), Utils.PUBLIC_STATIC);

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();

		QName qname = getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.get(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(
					new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		cl.addMethod("collectNS", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addParameter(types.getClass(Consumer.class, types.get(String.class)), "c")
				.getBody().get()
				.addStatement(new MethodCallExpr(new NameExpr("c"), "accept", Utils.list(new StringLiteralExpr("http://www.w3.org/2001/XMLSchema"))));

		cl.addMethod("write", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(XMLStreamWriter.class), "w").addParameter(types.get(t), "t")
				.getBody().get()
				.addStatement(new MethodCallExpr(new NameExpr("w"), "writeCharacters", Utils.list(new MethodCallExpr(null, "toString", Utils.list(new NameExpr("t"))))));

		cl.addMethod("read", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(XMLStreamReader.class), "r").setType(types.get(t))
				.getBody().get()
				.addStatement(Utils.assign(types.get(t), "o", new NullLiteralExpr()))
				.addStatement(new WhileStmt(new MethodCallExpr(new NameExpr("r"), "hasNext"),
						new BlockStmt()
								.addStatement(Utils.assign(types.get(int.class), "n", new MethodCallExpr(new NameExpr("r"), "next")))
								.addStatement(new IfStmt(
										new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "CHARACTERS"), BinaryExpr.Operator.EQUALS),
										new BlockStmt()
												.addStatement(new AssignExpr(new NameExpr("o"), new MethodCallExpr(null, "toObject", Utils.list(new MethodCallExpr(new TypeExpr(types.get(StrReader.class)), "read", Utils.list(new NameExpr("r"))))), AssignExpr.Operator.ASSIGN))
												.addStatement(new AssignExpr(new NameExpr("n"), new MethodCallExpr(new NameExpr("r"), "getEventType"), AssignExpr.Operator.ASSIGN)),
										null))
								.addStatement(new IfStmt(
										new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "CHARACTERS"), BinaryExpr.Operator.EQUALS),
										new ReturnStmt(new NameExpr("o")), null))))
				.addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("EOF")))));

		NodeList<SwitchEntry> list = xml.entries().stream().map(e -> new SwitchEntry().setLabels(Utils.list(new NameExpr(e.name()))).addStatement(new ReturnStmt(new StringLiteralExpr(e.value())))).collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("Unsupported enum constant"))))));
		cl.addMethod("toString", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(xml.type()), "o")
				.setType(types.get(String.class))
				.getBody().get()
				.addStatement(new SwitchStmt(new NameExpr("o"), list));

		list = xml.entries().stream().map(e -> new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(e.value()))).addStatement(new ReturnStmt(new FieldAccessExpr(new TypeExpr(types.get(t)), e.name())))).collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new BinaryExpr(new StringLiteralExpr("Unsupported enum value "), new NameExpr("s"), BinaryExpr.Operator.PLUS))))));
		cl.addMethod("toObject", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(String.class), "s")
				.setType(types.get(xml.type()))
				.getBody().get()
				.addStatement(new SwitchStmt(new NameExpr("s"), list));

		out.save(cu);
	}

	/**
	 * @param typeModel
	 * @throws MojoExecutionException
	 */
	private void buildHandler(XmlTypeComplex xml) throws MojoExecutionException {
		TypeModel t = xml.type();
		logger.info("process {}", t);

		// TODO get metadata

		String n = handlers.get(xml);
		int j = n.lastIndexOf('.');
		if (j > 0)
			n = n.substring(j + 1);

		CompilationUnit cu = newCu();
		TypeCache types = new TypeCache(cu, existingClass);
		ClassOrInterfaceDeclaration cl = cu.addClass(n)
				.addImplementedType(types.getClass(XmlHandler.class, types.get(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, n), "INSTANCE", new ObjectCreationExpr(null, new ClassOrInterfaceType(null, n), Utils.list()), Utils.PUBLIC_STATIC);

		int i = 0;
		for (XmlElement e : xml.getAttributes())
			cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$a" + i++, new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);
		i = 0;
		for (XmlElement e : xml.getElements())
			cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$e" + i++, new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();
		QName qname = getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.get(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(
					new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		b = cl.addMethod("collectNS", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addParameter(types.getClass(Consumer.class, types.get(String.class)), "c")
				.getBody().get();

		for (XmlElement a : xml.getAttributes()) {
			if (!a.ns().isEmpty())
				b.addStatement(new MethodCallExpr(new NameExpr("c"), "accept", Utils.list(new StringLiteralExpr(a.ns()))));
		}
		for (XmlElement a : xml.getElements())
			b.addStatement(new MethodCallExpr(new NameExpr("c"), "accept", Utils.list(new StringLiteralExpr(a.ns()))));

		buildWriter(cl, types, xml);
		buildReader(cl, types, xml);

		out.save(cu);
	}

	private void buildWriter(ClassOrInterfaceDeclaration cl, TypeCache types, XmlTypeComplex xml) {
		BlockStmt b = cl.addMethod("write", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.get(XMLStreamException.class).asClassOrInterfaceType())
				.addParameter(types.get(XMLStreamWriter.class), "w").addParameter(types.get(xml.type()), "t")
				.getBody().get();

		if (!xml.getAttributes().isEmpty()) {
			b.addStatement(new VariableDeclarationExpr(types.get(String.class), "s"));
			for (XmlElement e : xml.getAttributes()) {
				Expression v = new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), "INSTANCE"), "toString", Utils.list(new MethodCallExpr(new NameExpr("t"), e.getter())));
				v = new EnclosedExpr(new AssignExpr(new NameExpr("s"), v, AssignExpr.Operator.ASSIGN));
				b.addStatement(new IfStmt(
						new BinaryExpr(v, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("w"), "writeAttribute", Utils.list(Utils.text(e.ns()), Utils.text(e.name()), new NameExpr("s")))),
						null));
			}
		}

		if (xml.getElements() != null) {
			for (XmlElement e : xml.getElements()) {
				b.addStatement(
						new BlockStmt().addStatement(Utils.assign(types.get(e.type()), "o", new MethodCallExpr(new NameExpr("t"), e.getter())))
								.addStatement(new IfStmt(new BinaryExpr(new NameExpr("o"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
										new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("w"), "writeStartElement", Utils.list(Utils.text(e.ns()), Utils.text(e.name()))))
												.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), "INSTANCE"), "write", Utils.list(new NameExpr("w"), new NameExpr("o"))))
												.addStatement(new MethodCallExpr(new NameExpr("w"), "writeEndElement", Utils.list())),
										null)));
			}
		}
		if (xml.getValue() != null) {
			XmlElement value = xml.getValue();
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "writeCharacters", Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(value.xmlType()))), "INSTANCE"), "toString", Utils.list(new MethodCallExpr(new NameExpr("t"), value.getter()))))));
		}
	}

	/**
	 * @param cl
	 * @param types
	 * @param xml
	 */
	private void buildReader(ClassOrInterfaceDeclaration cl, TypeCache types, XmlTypeComplex xml) {
		Expression create;
		if ("".equals(xml.factory().method))
			create = new ObjectCreationExpr(null, types.getClass(xml.factory().clazz), Utils.list());
		else
			create = new MethodCallExpr(new TypeExpr(types.getClass(xml.factory().clazz)), xml.factory().method);

		BlockStmt b = cl.addMethod("read", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.get(XMLStreamException.class).asClassOrInterfaceType())
				.addParameter(types.get(XMLStreamReader.class), "r").setType(types.get(xml.type()))
				.getBody().get()
				.addStatement(Utils.assign(types.get(xml.type()), "o", create));

		if (!xml.getAttributes().isEmpty()) {
			IfStmt i = null;
			int c = 0;
			for (XmlElement e : xml.getAttributes()) {
				i = new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$a" + c++), "equals", Utils.list(new NameExpr("n"))),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(), Utils.list(
								new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), "INSTANCE"), "toObject", Utils.list(
										new MethodCallExpr(new NameExpr("r"), "getAttributeValue", Utils.list(new NameExpr("i")))))))),
						i);
			}
			b.addStatement(new ForStmt(
					Utils.list(Utils.assign(types.get("int"), "i", new IntegerLiteralExpr("0"))),
					new BinaryExpr(new NameExpr("i"), new MethodCallExpr(new NameExpr("r"), "getAttributeCount"), BinaryExpr.Operator.LESS),
					Utils.list(new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT)),
					new BlockStmt().addStatement(Utils.assign(types.get(QName.class), "n", new MethodCallExpr(new NameExpr("r"), "getAttributeName", Utils.list(new NameExpr("i"))))).addStatement(i)));
		}

		IfStmt i = new IfStmt(
				new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "END_ELEMENT"), BinaryExpr.Operator.EQUALS),
				new ReturnStmt(new NameExpr("o")), null);

		if (xml.getElements().iterator().hasNext()) {
			int c = 0;
			IfStmt j = null;
			for (XmlElement e : xml.getElements()) {
				j = new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$e" + c++), "equals", Utils.list(new NameExpr("q"))),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(), Utils.list(
								new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), "INSTANCE"), "read", Utils.list(new NameExpr("r")))))),
						j);
			}

			i = new IfStmt(
					new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "START_ELEMENT"), BinaryExpr.Operator.EQUALS),
					new BlockStmt().addStatement(Utils.assign(types.get(QName.class), "q", new MethodCallExpr(new NameExpr("r"), "getName"))).addStatement(j), i);
		}

		BlockStmt w = new BlockStmt().addStatement(Utils.assign(types.get("int"), "n", new MethodCallExpr(new NameExpr("r"), "next")));
		if (xml.getValue() != null) {
			XmlElement v = xml.getValue();
			w.addStatement(new IfStmt(
					new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "CHARACTERS"), BinaryExpr.Operator.EQUALS),
					new BlockStmt()
							.addStatement(new MethodCallExpr(new NameExpr("o"), v.setter(), Utils.list(
									new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(v.xmlType()))), "INSTANCE"), "toObject", Utils.list(new MethodCallExpr(new TypeExpr(types.get(StrReader.class)), "read", Utils.list(new NameExpr("r"))))))))
							.addStatement(new AssignExpr(new NameExpr("n"), new MethodCallExpr(new NameExpr("r"), "getEventType"), AssignExpr.Operator.ASSIGN)),
					null));
		}
		b.addStatement(
				new WhileStmt(new MethodCallExpr(new NameExpr("r"), "hasNext"),
						w.addStatement(i)))
				.addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("EOF")))));
	}

	private void buildContext() throws MojoExecutionException {
		CompilationUnit cu = newCu();
		TypeCache types = new TypeCache(cu, existingClass);
		ClassOrInterfaceDeclaration cl = cu.addClass("JaxbContextFactory")
				.addExtendedType(types.getClass(ContextFactory.class));
		Map<String, List<TypeModel>> classes = new HashMap<>();

		BlockStmt b = new BlockStmt();
		for (Entry<String, XmlType> t : xmlLoader.entries()) {
			if ("http://www.w3.org/2001/XMLSchema".equals(t.getValue().ns()))
				continue;
			TypeModel type = loader.get(t.getKey());
			b.addStatement(new MethodCallExpr(null, "register", Utils.list(new ClassExpr(types.get(t.getKey())), new FieldAccessExpr(new TypeExpr(types.get(handlers.get(t.getValue()))), "INSTANCE"))));
			classes.computeIfAbsent(type.packageName(), k -> new ArrayList<>()).add(type);
		}

		int i = 0;
		NodeList<SwitchEntry> entries = new NodeList<>();
		for (Entry<String, List<TypeModel>> e : classes.entrySet()) {
			String n = "p$" + i++;
			Expression c = new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList", e.getValue().stream().map(v -> new ClassExpr(types.get(v))).collect(Collectors.toCollection(() -> Utils.list())));
			cl.addFieldWithInitializer(types.getClass(Collection.class, types.getClass(Class.class, TypeCache.ANY)), n, c, Utils.PSF);
			entries.add(new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(e.getKey()))).addStatement(new ReturnStmt(new NameExpr(n))));
		}

		entries.add(new SwitchEntry().addStatement(new ReturnStmt(new MethodCallExpr(new TypeExpr(types.get(Collections.class)), "emptyList"))));

		cl.addConstructor(Modifier.Keyword.PUBLIC).setBody(b);
		cl.addMethod("getClasses", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.setType(types.getClass(Collection.class, types.getClass(Class.class, TypeCache.ANY)))
				.addParameter(types.get(String.class), "contextPackage")
				.getBody().get()
				.addStatement(new SwitchStmt(new NameExpr("contextPackage"), entries));
		out.save(cu);

		Path path = Paths.get(resources, "META-INF", "services", JAXBContextFactory.class.getName());
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			w.append(packageName).write(".JaxbContextFactory\n");
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
	}

	/**
	 * @param t
	 */
	private QName getRootQN(TypeModel t) {
		AnnotationModel r = t.annotation(XmlRootElement.class).orElse(null);
		if (r == null)
			return null;
		// TODO
//		XmlSchema
		return new QName(
				r.value("namespace").filter(v -> !"##default".equals(v)).orElse(""),
				r.value("name").filter(v -> !"##default".equals(v)).orElse(t.simpleName()));
	}

}