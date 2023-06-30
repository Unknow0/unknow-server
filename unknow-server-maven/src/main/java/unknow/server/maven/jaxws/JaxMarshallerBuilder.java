/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.UnknownType;

import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.Marshaler;
import unknow.server.jaxws.MarshalerRegistry;
import unknow.server.jaxws.XMLNsCollector;
import unknow.server.jaxws.XMLOutput;
import unknow.server.jaxws.XMLWriter;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.binding.XmlEnum;
import unknow.server.maven.jaxws.binding.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.binding.XmlObject;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
import unknow.server.maven.model.TypeModel;

public class JaxMarshallerBuilder {
	private static final NameExpr R = new NameExpr("R");
	private static final NameExpr E = new NameExpr("e");
	private static final NameExpr C = new NameExpr("c");

	private final TypeCache types;
	private final ClassOrInterfaceDeclaration clazz;
	private final BlockStmt init;
	private final Set<TypeModel> processed;

	public JaxMarshallerBuilder(CompilationUnit cu, TypeCache types) {
		this.types = types;
		this.clazz = cu.addClass("Marshallers", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
		this.processed = new HashSet<>();
		clazz.addFieldWithInitializer(types.getClass(MarshalerRegistry.class), "R", new ObjectCreationExpr(null, types.getClass(MarshalerRegistry.class), Utils.list()),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		init = clazz.addStaticInitializer();

		clazz.addMethod("marshall", Keyword.PUBLIC, Keyword.STATIC, Keyword.FINAL).addParameter(types.getClass(Envelope.class), "e")
				.addParameter(types.getClass(Writer.class), "w").addThrownException(types.getClass(IOException.class)).getBody().get()
				.addStatement(Utils.create(types.getClass(XMLNsCollector.class), "c", Utils.list()))
				.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.getClass(MarshalerRegistry.class)), "ENVELOPE"), "marshall",
						Utils.list(R, E, C)))
				.addStatement(
						new TryStmt(
								Utils.list(
										new AssignExpr(new VariableDeclarationExpr(types.getClass(XMLWriter.class), "out"),
												new ObjectCreationExpr(null, types.getClass(XMLOutput.class),
														Utils.list(new NameExpr("w"), new MethodCallExpr(C, "buildNsMapping"))),
												AssignExpr.Operator.ASSIGN)),
								new BlockStmt().addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.getClass(MarshalerRegistry.class)), "ENVELOPE"),
										"marshall", Utils.list(R, E, new NameExpr("out")))),
								Utils.list(), null));
		clazz.addMethod("marshall", Keyword.PUBLIC, Keyword.STATIC, Keyword.FINAL).addParameter(types.getClass(Object.class), "e")
				.addParameter(types.getClass(Writer.class), "w").addThrownException(types.getClass(IOException.class))
				.addSingleMemberAnnotation(SuppressWarnings.class, Utils.text("unchecked")).getBody().get()
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(Marshaler.class, types.getClass(Object.class)), "m"),
						new MethodCallExpr(R, "get", Utils.list(new CastExpr(types.getClass(Class.class, types.getClass(Object.class)), new MethodCallExpr(E, "getClass")))),
						AssignExpr.Operator.ASSIGN))
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(XMLNsCollector.class), "c"),
						new ObjectCreationExpr(null, types.getClass(XMLNsCollector.class), Utils.list()), AssignExpr.Operator.ASSIGN))
				.addStatement(
						new MethodCallExpr(new NameExpr("m"), "marshall", Utils.list(R, E, C)))
				.addStatement(
						new TryStmt(
								Utils.list(
										new AssignExpr(new VariableDeclarationExpr(types.getClass(XMLWriter.class), "out"),
												new ObjectCreationExpr(null, types.getClass(XMLOutput.class),
														Utils.list(new NameExpr("w"), new MethodCallExpr(C, "buildNsMapping"))),
												AssignExpr.Operator.ASSIGN)),
								new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("m"), "marshall", Utils.list(R, E, new NameExpr("out")))), Utils.list(), null));
	}

	public void add(XmlEnum type) {
		if (processed.contains(type.javaType()))
			return;
		processed.add(type.javaType());

		NodeList<SwitchEntry> list = Utils.list();
		for (XmlEnumEntry e : type.entries)
			list.add(new SwitchEntry(Utils.list(new NameExpr(e.name)), SwitchEntry.Type.STATEMENT_GROUP, Utils.list(new ReturnStmt(Utils.text(e.value)))));
		list.add(new SwitchEntry(Utils.list(), SwitchEntry.Type.STATEMENT_GROUP, Utils.list(new ReturnStmt(Utils.text("")))));
		clazz.addMethod(type.convertMethod, Keyword.PRIVATE, Keyword.STATIC, Keyword.FINAL).addParameter(types.get(type.javaType().name()), "e")
				.setType(types.getClass(String.class)).getBody().get().addStatement(new SwitchStmt(new NameExpr("e"), list));
	}

	public void add(XmlObject type) {
		if (processed.contains(type.javaType()))
			return;
		processed.add(type.javaType());

		BlockStmt b = new BlockStmt();
		for (XmlField<?> a : type.attrs()) {
			TypeModel jtype = a.type().javaType();
			if (!a.type().javaType().isPrimitive()) {
				b.addStatement(Utils.assign(types.get(jtype.name()), a.name(), new MethodCallExpr(new NameExpr("t"), a.getter)));
				b.addStatement(new IfStmt(new BinaryExpr(new NameExpr(a.name()), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("w"), "attribute",
								Utils.list(Utils.text(a.name()), Utils.text(a.ns()), a.type().toString(types, new NameExpr(a.name()))))),
						null));
			} else
				b.addStatement(new MethodCallExpr(new NameExpr("w"), "attribute",
						Utils.list(Utils.text(a.name()), Utils.text(a.ns()), a.type().toString(types, new MethodCallExpr(new NameExpr("t"), a.getter)))));
		}
		for (XmlField<?> f : type.elems()) {
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "startElement", Utils.list(Utils.text(f.name()), Utils.text(f.ns()))));
			if (f.type() instanceof XmlObject) {
				XmlObject t = (XmlObject) f.type();
				add(t);
				b.addStatement(Utils.assign(types.get(t.javaType().name()), f.name(), new MethodCallExpr(new NameExpr("t"), f.getter)));
				b.addStatement(new IfStmt(new BinaryExpr(new NameExpr(f.name()), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new ExpressionStmt(new MethodCallExpr(new MethodCallExpr(new NameExpr("R"), "get", Utils.list(new ClassExpr(types.get(f.type().javaType().name())))),
								"marshall", Utils.list(new NameExpr("m"), new NameExpr(f.name()), new NameExpr("w")))),
						null));
			} else {
				if (f.type() instanceof XmlEnum)
					add((XmlEnum) f.type());
				if (!f.type().javaType().isPrimitive()) {
					b.addStatement(Utils.assign(types.get(f.type().javaType().name()), f.name(), new MethodCallExpr(new NameExpr("t"), f.getter)));
					b.addStatement(new IfStmt(new BinaryExpr(new NameExpr(f.name()), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
							new ExpressionStmt(
									new MethodCallExpr(new NameExpr("w"), "text", Utils.list(f.type().toString(types, new MethodCallExpr(new NameExpr("t"), f.getter))))),
							null));
				} else
					b.addStatement(new MethodCallExpr(new NameExpr("w"), "text", Utils.list(f.type().toString(types, new MethodCallExpr(new NameExpr("t"), f.getter)))));
			}
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "endElement", Utils.list(Utils.text(f.name()), Utils.text(f.ns()))));
		}
		if (type.value() != null) {
			XmlField<?> f = type.value();
			if (!f.type().javaType().isPrimitive()) {
				b.addStatement(Utils.assign(types.get(f.type().javaType().name()), "$v$", new MethodCallExpr(new NameExpr("t"), f.getter)));
				b.addStatement(
						new IfStmt(new BinaryExpr(new NameExpr("$v$"), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
								new ExpressionStmt(
										new MethodCallExpr(new NameExpr("w"), "text", Utils.list(f.type().toString(types, new MethodCallExpr(new NameExpr("t"), f.getter))))),
								null));
			} else
				b.addStatement(new MethodCallExpr(new NameExpr("w"), "text", Utils.list(f.type().toString(types, new MethodCallExpr(new NameExpr("t"), f.getter)))));
		}

		init.addStatement(new MethodCallExpr(new NameExpr("R"), "register", Utils.list(new ClassExpr(types.get(type.javaType().name())),
				new LambdaExpr(Utils.list(new Parameter(new UnknownType(), "m"), new Parameter(new UnknownType(), "t"), new Parameter(new UnknownType(), "w")), b))));
	}
}