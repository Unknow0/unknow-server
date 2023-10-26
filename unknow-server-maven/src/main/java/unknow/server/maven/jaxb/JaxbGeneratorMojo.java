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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
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
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import unknow.server.jaxb.ContextFactory;
import unknow.server.jaxb.StrReader;
import unknow.server.jaxb.UnmarshallerImpl;
import unknow.server.jaxb.XmlHandler;
import unknow.server.jaxb.XmlRootHandler;
import unknow.server.jaxb.XmlSimpleHandler;
import unknow.server.jaxb.handler.BigDecimalHandler;
import unknow.server.jaxb.handler.BigIntegerHandler;
import unknow.server.jaxb.handler.BooleanHandler;
import unknow.server.jaxb.handler.ByteHandler;
import unknow.server.jaxb.handler.CharHandler;
import unknow.server.jaxb.handler.DoubleHandler;
import unknow.server.jaxb.handler.DurationHandler;
import unknow.server.jaxb.handler.FloatHandler;
import unknow.server.jaxb.handler.IntHandler;
import unknow.server.jaxb.handler.LocalDateHandler;
import unknow.server.jaxb.handler.LocalDateTimeHandler;
import unknow.server.jaxb.handler.LocalTimeHandler;
import unknow.server.jaxb.handler.LongHandler;
import unknow.server.jaxb.handler.OffsetDateTimeHandler;
import unknow.server.jaxb.handler.PeriodHandler;
import unknow.server.jaxb.handler.ShortHandler;
import unknow.server.jaxb.handler.StringHandler;
import unknow.server.jaxb.handler.ZonedDateTimeHandler;
import unknow.server.maven.AbstractGeneratorMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model_xml.XmlCollection;
import unknow.server.maven.model_xml.XmlElement;
import unknow.server.maven.model_xml.XmlEnum;
import unknow.server.maven.model_xml.XmlLoader;
import unknow.server.maven.model_xml.XmlType;
import unknow.server.maven.model_xml.XmlTypeComplex;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxb-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxbGeneratorMojo extends AbstractGeneratorMojo {
	private static final Logger logger = LoggerFactory.getLogger(JaxbGeneratorMojo.class);

	private static final String INSTANCE = "INSTANCE";

	private final Map<XmlType, String> handlers = new HashMap<>();
	private final XmlLoader xmlLoader = new XmlLoader();

	public JaxbGeneratorMojo() {
		handlers.put(XmlLoader.BOOLEAN, BooleanHandler.class.getName());
		handlers.put(XmlLoader.BYTE, ByteHandler.class.getName());
		handlers.put(XmlLoader.SHORT, ShortHandler.class.getName());
		handlers.put(XmlLoader.CHAR, CharHandler.class.getName());
		handlers.put(XmlLoader.INT, IntHandler.class.getName());
		handlers.put(XmlLoader.LONG, LongHandler.class.getName());
		handlers.put(XmlLoader.FLOAT, FloatHandler.class.getName());
		handlers.put(XmlLoader.DOUBLE, DoubleHandler.class.getName());
		handlers.put(XmlLoader.STRING, StringHandler.class.getName());
		handlers.put(XmlLoader.BIGINT, BigIntegerHandler.class.getName());
		handlers.put(XmlLoader.BIGDEC, BigDecimalHandler.class.getName());
		handlers.put(XmlLoader.BIGDEC, BigDecimalHandler.class.getName());

		handlers.put(XmlLoader.LOCALDATE, LocalDateHandler.class.getName());
		handlers.put(XmlLoader.LOCALDATETIME, LocalDateTimeHandler.class.getName());
		handlers.put(XmlLoader.LOCALTIME, LocalTimeHandler.class.getName());
		handlers.put(XmlLoader.OFFSETDATETIME, OffsetDateTimeHandler.class.getName());
		handlers.put(XmlLoader.ZONEDDATETIME, ZonedDateTimeHandler.class.getName());
		handlers.put(XmlLoader.DURATION, DurationHandler.class.getName());
		handlers.put(XmlLoader.PERIOD, PeriodHandler.class.getName());

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
			if (!XmlLoader.XS.equals(t.name().getNamespaceURI()))
				handlers.put(t, packageName + "." + t.type().simpleName() + "_" + i++);
		}
		// default handers

		for (XmlType t : xmlLoader.types()) {
			if (XmlLoader.XS.equals(t.name().getNamespaceURI()))
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
		ClassOrInterfaceDeclaration cl = cu.addClass(n).addImplementedType(types.getClass(XmlSimpleHandler.class, types.get(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, n), INSTANCE, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, n), Utils.list()),
				Utils.PUBLIC_STATIC);

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();

		QName qname = getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.get(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(new ObjectCreationExpr(null, types.getClass(QName.class),
					Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		NodeList<SwitchEntry> list = xml.entries().stream()
				.map(e -> new SwitchEntry().setLabels(Utils.list(new NameExpr(e.name()))).addStatement(new ReturnStmt(new StringLiteralExpr(e.value()))))
				.collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(
				new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("Unsupported enum constant"))))));
		cl.addMethod("toString", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(xml.type()), "o").setType(types.get(String.class)).createBody().addStatement(new SwitchStmt(new NameExpr("o"), list));

		list = xml.entries().stream().map(e -> new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(e.value())))
				.addStatement(new ReturnStmt(new FieldAccessExpr(new TypeExpr(types.get(t)), e.name())))).collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class),
				Utils.list(new BinaryExpr(new StringLiteralExpr("Unsupported enum value "), new NameExpr("s"), BinaryExpr.Operator.PLUS))))));
		cl.addMethod("toObject", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(XMLStreamException.class))
				.addParameter(types.get(String.class), "s").setType(types.get(xml.type())).createBody().addStatement(new SwitchStmt(new NameExpr("s"), list));

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
		ClassOrInterfaceDeclaration cl = cu.addClass(n).addImplementedType(types.getClass(XmlHandler.class, types.get(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, n), INSTANCE, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, n), Utils.list()),
				Utils.PUBLIC_STATIC);

		int i = 0;
		for (XmlElement e : xml.getAttributes())
			cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$a" + i++,
					new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);
		i = 0;
		Map<TypeModel, String> emptyArray = new HashMap<>();
		for (XmlElement e : xml.getElements()) {
			cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$e" + i++,
					new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);
			if (e.xmlType() instanceof XmlCollection && e.type().isArray()) {
				String name = "EMPTY$" + emptyArray.size();
				emptyArray.put(e.type(), name);
				cl.addFieldWithInitializer(types.get(e.type()), name, new ArrayInitializerExpr(), Utils.PSF);
			}
		}

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();
		QName qname = getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.get(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(new ObjectCreationExpr(null, types.getClass(QName.class),
					Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		buildWriter(cl, types, xml);
		buildReader(cl, types, xml, emptyArray);

		out.save(cu);
	}

	private void buildWriter(ClassOrInterfaceDeclaration cl, TypeCache types, XmlTypeComplex xml) {
		BlockStmt b = cl.addMethod("write", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.get(XMLStreamException.class).asClassOrInterfaceType())
				.addParameter(types.get(XMLStreamWriter.class), "w").addParameter(types.get(xml.type()), "t").addParameter(types.get(Marshaller.Listener.class), "listener")
				.createBody();
		xml.type().asClass().method("beforeMarshal", loader.get(Marshaller.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "beforeMarshal", Utils.list(new NameExpr("listener")))));
		b.addStatement(new IfStmt(new BinaryExpr(new NameExpr("listener"), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
				new ExpressionStmt(new MethodCallExpr(new NameExpr("listener"), "beforeMarshal", Utils.list(new NameExpr("t")))), null));

		if (!xml.getAttributes().isEmpty()) {
			b.addStatement(new VariableDeclarationExpr(types.get(String.class), "s"));
			for (XmlElement e : xml.getAttributes()) {
				Expression v = new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), INSTANCE), "toString",
						Utils.list(new MethodCallExpr(new NameExpr("t"), e.getter())));
				v = new EnclosedExpr(new AssignExpr(new NameExpr("s"), v, AssignExpr.Operator.ASSIGN));
				NodeList<Expression> l = Utils.list();
				if (!e.ns().isEmpty())
					l.add(Utils.text(e.ns()));
				l.add(Utils.text(e.name()));
				l.add(new NameExpr("s"));
				b.addStatement(new IfStmt(new BinaryExpr(v, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("w"), "writeAttribute", l)), null));
			}
		}

		int i = 0;
		for (XmlElement e : xml.getElements()) {
			String n = "o$" + i++;
			BlockStmt w;

			XmlType t = e.xmlType();
			if (t instanceof XmlCollection) {
				XmlCollection c = (XmlCollection) t;
				BlockStmt p = new BlockStmt()
						.addStatement(new IfStmt(new BinaryExpr(new NameExpr("e"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ContinueStmt(), null))
						.addStatement(new MethodCallExpr(new NameExpr("w"), "writeStartElement", Utils.list(Utils.text(e.ns()), Utils.text(e.name()))))
						.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(c.component()))), INSTANCE), "write",
								Utils.list(new NameExpr("w"), new NameExpr("e"), new NameExpr("listener"))))
						.addStatement(new MethodCallExpr(new NameExpr("w"), "writeEndElement", Utils.list()));

				if (t.type().isArray()) {
					p.addStatement(0, Utils.assign(types.get(c.component().type()), "e", new ArrayAccessExpr(new NameExpr(n), new NameExpr("i"))));
					w = new BlockStmt().addStatement(new ForStmt(Utils.list(Utils.assign(types.get("int"), "i", new IntegerLiteralExpr("0"))),
							new BinaryExpr(new NameExpr("i"), new FieldAccessExpr(new NameExpr(n), "length"), BinaryExpr.Operator.LESS),
							Utils.list(new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT)), p));
				} else {
					w = new BlockStmt().addStatement(new ForEachStmt(new VariableDeclarationExpr(types.get(c.component().type()), "e"), new NameExpr(n), p));
				}
			} else {
				w = new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("w"), "writeStartElement", Utils.list(Utils.text(e.ns()), Utils.text(e.name()))))
						.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), INSTANCE), "write",
								Utils.list(new NameExpr("w"), new NameExpr(n), new NameExpr("listener"))))
						.addStatement(new MethodCallExpr(new NameExpr("w"), "writeEndElement", Utils.list()));
			}
			b.addStatement(Utils.assign(types.get(e.type()), n, new MethodCallExpr(new NameExpr("t"), e.getter())))
					.addStatement(new IfStmt(new BinaryExpr(new NameExpr(n), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS), w, null));
		}
		if (xml.getValue() != null) {
			XmlElement value = xml.getValue();
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "writeCharacters",
					Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(value.xmlType()))), INSTANCE), "toString",
							Utils.list(new MethodCallExpr(new NameExpr("t"), value.getter()))))));
		}

		xml.type().asClass().method("afterMarshal", loader.get(Marshaller.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "afterMarshal", Utils.list(new NameExpr("listener")))));
		b.addStatement(new IfStmt(new BinaryExpr(new NameExpr("listener"), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
				new ExpressionStmt(new MethodCallExpr(new NameExpr("listener"), "afterMarshal", Utils.list(new NameExpr("t")))), null));
	}

	/**
	 * @param cl
	 * @param types
	 * @param xml
	 * @param emptyArray
	 */
	private void buildReader(ClassOrInterfaceDeclaration cl, TypeCache types, XmlTypeComplex xml, Map<TypeModel, String> emptyArray) {
		Expression create;
		if ("".equals(xml.factory().method))
			create = new ObjectCreationExpr(null, types.getClass(xml.factory().clazz), Utils.list());
		else
			create = new MethodCallExpr(new TypeExpr(types.getClass(xml.factory().clazz)), xml.factory().method);

		BlockStmt b = cl.addMethod("read", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.get(XMLStreamException.class).asClassOrInterfaceType())
				.addParameter(types.get(XMLStreamReader.class), "r").setType(types.get(xml.type())).addParameter(types.get(Object.class), "parent")
				.addParameter(types.get(UnmarshallerImpl.class), "listener").createBody().addStatement(Utils.assign(types.get(xml.type()), "o", create));
		xml.type().asClass().method("beforeUnmarshal", loader.get(Unmarshaller.class.getName()), loader.get(Object.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "beforeUnmarshal", Utils.list(new NameExpr("listener"), new NameExpr("parent")))));
		b.addStatement(new MethodCallExpr(new NameExpr("listener"), "beforeUnmarshal", Utils.list(new NameExpr("o"), new NameExpr("parent"))));

		for (XmlElement e : xml.getElements()) {
			if (!(e.xmlType() instanceof XmlCollection))
				continue;
			Type t = e.type().isArray() ? types.getClass(List.class, types.get(e.type().asArray().type())) : types.get(e.type());
			b.addStatement(Utils.assign(t, "l$" + e.name(), new NullLiteralExpr()));
		}
		if (!xml.getAttributes().isEmpty()) {
			Statement i = null;
			int c = 0;
			for (XmlElement e : xml.getAttributes()) {
				i = new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$a" + c++), "equals", Utils.list(new NameExpr("n"))),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(),
								Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), INSTANCE), "toObject",
										Utils.list(new MethodCallExpr(new NameExpr("r"), "getAttributeValue", Utils.list(new NameExpr("i")))))))),
						i);
			}
			b.addStatement(new ForStmt(Utils.list(Utils.assign(types.get("int"), "i", new IntegerLiteralExpr("0"))),
					new BinaryExpr(new NameExpr("i"), new MethodCallExpr(new NameExpr("r"), "getAttributeCount"), BinaryExpr.Operator.LESS),
					Utils.list(new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT)),
					new BlockStmt()
							.addStatement(Utils.assign(types.get(QName.class), "n", new MethodCallExpr(new NameExpr("r"), "getAttributeName", Utils.list(new NameExpr("i")))))
							.addStatement(i)));
		}

		BlockStmt r = new BlockStmt();
		for (XmlElement e : xml.getElements()) {
			if (!(e.xmlType() instanceof XmlCollection))
				continue;
			Expression v = new NameExpr("l$" + e.name());
			if (e.type().isArray())
				v = new MethodCallExpr(v, "toArray", Utils.list(new NameExpr(emptyArray.get(e.type()))));
			r.addStatement(new IfStmt(new BinaryExpr(new NameExpr("l$" + e.name()), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
					new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(), Utils.list(v))), null));
		}

		xml.type().asClass().method("afterUnmarshal", loader.get(Unmarshaller.class.getName()), loader.get(Object.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "beforeUnmarshal", Utils.list(new NameExpr("listener"), new NameExpr("parent")))));

		IfStmt i = new IfStmt(
				new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "END_ELEMENT"), BinaryExpr.Operator.EQUALS),
				r.addStatement(new MethodCallExpr(new NameExpr("listener"), "afterUnmarshal", Utils.list(new NameExpr("o"), new NameExpr("parent"))))
						.addStatement(new ReturnStmt(new NameExpr("o"))),
				null);

		if (xml.getElements().iterator().hasNext()) {
			int c = 0;
			Statement j = new ExpressionStmt(new MethodCallExpr(new TypeExpr(types.get(XmlHandler.class)), "skipTag", Utils.list(new NameExpr("r"))));
			for (XmlElement e : xml.getElements()) {
				Statement s;
				if (e.xmlType() instanceof XmlCollection) {
					XmlCollection t = (XmlCollection) e.xmlType();
					NameExpr n = new NameExpr("l$" + e.name());
					s = new BlockStmt()
							.addStatement(new IfStmt(new BinaryExpr(n, new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
									new ExpressionStmt(new AssignExpr(n,
											new ObjectCreationExpr(null, types.getClass(e.type().isAssignableTo(Set.class) ? HashSet.class : ArrayList.class, TypeCache.EMPTY),
													Utils.list()),
											AssignExpr.Operator.ASSIGN)),
									null))
							.addStatement(new MethodCallExpr(n, "add",
									Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(t.component()))), INSTANCE), "read",
											Utils.list(new NameExpr("r"), new NameExpr("o"), new NameExpr("listener"))))));
				} else
					s = new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(),
							Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(e.xmlType()))), INSTANCE), "read",
									Utils.list(new NameExpr("r"), new NameExpr("o"), new NameExpr("listener"))))));

				j = new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$e" + c++), "equals", Utils.list(new NameExpr("q"))), s, j);
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
							.addStatement(new MethodCallExpr(new NameExpr("o"), v.setter(),
									Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(handlers.get(v.xmlType()))), INSTANCE), "toObject",
											Utils.list(new MethodCallExpr(new TypeExpr(types.get(StrReader.class)), "read", Utils.list(new NameExpr("r"))))))))
							.addStatement(new AssignExpr(new NameExpr("n"), new MethodCallExpr(new NameExpr("r"), "getEventType"), AssignExpr.Operator.ASSIGN)),
					null));
		}
		b.addStatement(new WhileStmt(new MethodCallExpr(new NameExpr("r"), "hasNext"), w.addStatement(i)))
				.addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("EOF")))));
	}

	private void buildContext() throws MojoExecutionException {
		CompilationUnit cu = newCu();
		TypeCache types = new TypeCache(cu, existingClass);
		ClassOrInterfaceDeclaration cl = cu.addClass("JaxbContextFactory").addExtendedType(types.getClass(ContextFactory.class));
		Map<String, List<TypeModel>> classes = new HashMap<>();

		BlockStmt b = new BlockStmt();
		for (Entry<String, XmlType> t : xmlLoader.entries()) {
			String h = handlers.get(t.getValue());
			if (h == null || XmlLoader.XS.equals(t.getValue().name().getNamespaceURI()))
				continue;
			TypeModel type = loader.get(t.getKey());
			b.addStatement(new MethodCallExpr(null, "register", Utils.list(new ClassExpr(types.get(t.getKey())), new FieldAccessExpr(new TypeExpr(types.get(h)), INSTANCE))));
			classes.computeIfAbsent(type.packageName(), k -> new ArrayList<>()).add(type);
		}

		int i = 0;
		NodeList<SwitchEntry> entries = new NodeList<>();
		for (Entry<String, List<TypeModel>> e : classes.entrySet()) {
			String n = "p$" + i++;
			Expression c = new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList",
					e.getValue().stream().map(v -> new ClassExpr(types.get(v))).collect(Collectors.toCollection(() -> Utils.list())));
			cl.addFieldWithInitializer(types.getClass(Collection.class, types.getClass(Class.class, TypeCache.ANY)), n, c, Utils.PSF);
			entries.add(new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(e.getKey()))).addStatement(new ReturnStmt(new NameExpr(n))));
		}

		entries.add(new SwitchEntry().addStatement(new ReturnStmt(new MethodCallExpr(new TypeExpr(types.get(Collections.class)), "emptyList"))));

		cl.addConstructor(Modifier.Keyword.PUBLIC).setBody(b);
		cl.addMethod("getClasses", Utils.PUBLIC).addMarkerAnnotation(Override.class).setType(types.getClass(Collection.class, types.getClass(Class.class, TypeCache.ANY)))
				.addParameter(types.get(String.class), "contextPackage").createBody().addStatement(new SwitchStmt(new NameExpr("contextPackage"), entries));
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
		return new QName(r.member("namespace").map(v -> v.asLiteral()).filter(v -> !"##default".equals(v)).orElse(""),
				r.member("name").map(v -> v.asLiteral()).filter(v -> !"##default".equals(v)).orElse(t.simpleName()));
	}

}