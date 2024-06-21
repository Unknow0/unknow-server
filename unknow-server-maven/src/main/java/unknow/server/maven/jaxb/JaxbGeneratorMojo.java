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
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;

import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.annotation.XmlRootElement;
import unknow.server.jaxb.ContextFactory;
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
import unknow.server.maven.jaxb.builder.HandlerBuilder;
import unknow.server.maven.jaxb.model.XmlCollection;
import unknow.server.maven.jaxb.model.XmlLoader;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxb-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxbGeneratorMojo extends AbstractGeneratorMojo {

	private static final HandlerBuilder HANDLER = new HandlerBuilder();

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
		process(type -> {
			if (type.annotation(jakarta.xml.bind.annotation.XmlType.class).isPresent())
				xmlLoader.add(type);
		});

		int i = 0;
		List<XmlType> list = new ArrayList<>(xmlLoader.types());
		list.sort((a, b) -> a.name().toString().compareTo(b.name().toString()));
		for (XmlType t : list) {
			if (!XmlLoader.XS.equals(t.name().getNamespaceURI()) && !(t instanceof XmlCollection))
				handlers.put(t, packageName + "." + t.type().simpleName() + "_" + i++);
		}

		for (XmlType t : xmlLoader.types())
			buildHandler(t);

		buildContext();
	}

	private void buildHandler(XmlType xml) throws MojoExecutionException {
		String n = handlers.get(xml);
		if (n == null || XmlLoader.XS.equals(xml.name().getNamespaceURI()))
			return;

		int j = n.lastIndexOf('.');
		if (j > 0)
			n = n.substring(j + 1);

		CompilationUnit cu = newCu();
		TypeCache types = new TypeCache(cu, existingClass);
		HANDLER.process(cu.addClass(n), types, new HandlerContext(handlers, loader, xml));
		out.save(cu);
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
			b.addStatement(
					new MethodCallExpr(null, "register", Utils.list(new ClassExpr(types.get(t.getKey())), new FieldAccessExpr(new TypeExpr(types.get(h)), "INSTANCE"))));
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
	public static QName getRootQN(TypeModel t) {
		AnnotationModel r = t.annotation(XmlRootElement.class).orElse(null);
		if (r == null)
			return null;
		// TODO
//		XmlSchema
		return new QName(r.member("namespace").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !"##default".equals(v)).orElse(""),
				r.member("name").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !"##default".equals(v)).orElse(t.simpleName()));
	}
}