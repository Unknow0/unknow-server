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
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.UnknownType;

import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.Marshaller;
import unknow.server.jaxws.MarshallerRegistry;
import unknow.server.jaxws.XMLNsCollector;
import unknow.server.jaxws.XMLOutput;
import unknow.server.jaxws.XMLWriter;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;

public class JaxMarshallerBuilder {
	private static final NameExpr R = new NameExpr("R");
	private static final NameExpr E = new NameExpr("e");
	private static final NameExpr C = new NameExpr("c");

	private final TypeCache types;
	private final ClassOrInterfaceDeclaration clazz;
	private final BlockStmt init;
	private final Set<String> processed;

	public JaxMarshallerBuilder(CompilationUnit cu, TypeCache types) {
		this.types = types;
		this.clazz = cu.addClass("Marshallers", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
		this.processed = new HashSet<>();
		clazz.addFieldWithInitializer(types.get(MarshallerRegistry.class), "R", new ObjectCreationExpr(null, types.get(MarshallerRegistry.class), Utils.list()), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		init = clazz.addStaticInitializer();

		clazz.addMethod("marshall", Keyword.PUBLIC, Keyword.STATIC, Keyword.FINAL).addParameter(types.get(Envelope.class), "e").addParameter(types.get(Writer.class), "w")
				.addThrownException(types.get(IOException.class))
				.getBody().get()
				.addStatement(Utils.create(types.get(XMLNsCollector.class), "c", Utils.list()))
				.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(Marshaller.class)), "ENVELOPE"), "marshall", Utils.list(R, E, C)))
				.addStatement(new TryStmt(new NodeList<>(
						new AssignExpr(
								new VariableDeclarationExpr(types.get(XMLWriter.class), "out"), new ObjectCreationExpr(null, types.get(XMLOutput.class), new NodeList<>(new NameExpr("w"), new MethodCallExpr(C, "buildNsMapping"))), AssignExpr.Operator.ASSIGN)),
						new BlockStmt()
								.addStatement(new MethodCallExpr(
										new FieldAccessExpr(new TypeExpr(types.get(Marshaller.class)), "ENVELOPE"),
										"marshall", new NodeList<>(R, E, new NameExpr("out")))),
						new NodeList<>(), null));
		clazz.addMethod("marshall", Keyword.PUBLIC, Keyword.STATIC, Keyword.FINAL).addParameter(types.get(Object.class), "e").addParameter(types.get(Writer.class), "w")
				.addThrownException(types.get(IOException.class))
				.addSingleMemberAnnotation(SuppressWarnings.class, new StringLiteralExpr("unchecked"))
				.getBody().get()
				.addStatement(new AssignExpr(
						new VariableDeclarationExpr(types.get(Marshaller.class, types.get(Object.class)), "m"),
						new MethodCallExpr(R, "get", new NodeList<>(new CastExpr(types.get(Class.class, types.get(Object.class)), new MethodCallExpr(E, "getClass")))),
						AssignExpr.Operator.ASSIGN))
				.addStatement(new AssignExpr(
						new VariableDeclarationExpr(types.get(XMLNsCollector.class), "c"),
						new ObjectCreationExpr(null, types.get(XMLNsCollector.class), new NodeList<>()),
						AssignExpr.Operator.ASSIGN))
				.addStatement(new MethodCallExpr(new NameExpr("m"), "marshall", new NodeList<>(R, E, C)))
				.addStatement(new TryStmt(new NodeList<>(
						new AssignExpr(
								new VariableDeclarationExpr(types.get(XMLWriter.class), "out"), new ObjectCreationExpr(null, types.get(XMLOutput.class), new NodeList<>(new NameExpr("w"), new MethodCallExpr(C, "buildNsMapping"))), AssignExpr.Operator.ASSIGN)),
						new BlockStmt()
								.addStatement(new MethodCallExpr(
										new NameExpr("m"),
										"marshall", new NodeList<>(R, E, new NameExpr("out")))),
						new NodeList<>(), null));
	}

	public void add(XmlObject type) {
		if (processed.contains(type.clazz))
			return;
		processed.add(type.clazz);

		BlockStmt b = new BlockStmt();
		for (XmlField a : type.attrs)
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "attribute", new NodeList<>(new StringLiteralExpr(a.name), new StringLiteralExpr(a.ns), a.type.toString(types, new MethodCallExpr(new NameExpr("t"), a.getter)))));
		for (XmlField f : type.elems) {
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "startElement", new NodeList<>(new StringLiteralExpr(f.name), new StringLiteralExpr(f.ns))));
			if (f.type instanceof XmlObject)
				b.addStatement(new MethodCallExpr(new MethodCallExpr(new NameExpr("R"), "get", new NodeList<>(new ClassExpr(types.get(((XmlObject) f.type).clazz)))), "marshal", new NodeList<>()));
			else {
				Expression e = f.type.toString(types, new MethodCallExpr(new NameExpr("t"), f.getter));
				b.addStatement(new MethodCallExpr(new NameExpr("w"), "text", new NodeList<>(e)));
			}
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "endElement", new NodeList<>(new StringLiteralExpr(f.name), new StringLiteralExpr(f.ns))));
		}
		if (type.value != null) {
			Expression e = type.value.type.toString(types, new MethodCallExpr(new NameExpr("t"), type.value.getter));
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "text", new NodeList<>(e)));
		}

		init.addStatement(new MethodCallExpr(new NameExpr("R"), "register", new NodeList<>(
				new ClassExpr(types.get(type.clazz)),
				new LambdaExpr(
						new NodeList<>(new Parameter(new UnknownType(), "m"), new Parameter(new UnknownType(), "t"), new Parameter(new UnknownType(), "w")),
						b))));
	}
}