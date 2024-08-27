package unknow.server.maven.jaxb.builder;

import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import jakarta.xml.bind.JAXBException;
import unknow.server.jaxb.XmlRootHandler;
import unknow.server.jaxb.XmlSimpleHandler;
import unknow.server.maven.SourceBuilder.AbstractSourceBuilder;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxb.HandlerContext;
import unknow.server.maven.jaxb.JaxbGeneratorMojo;
import unknow.server.maven.jaxb.model.XmlEnum;
import unknow.server.maven.model.TypeModel;

public class HandlerEnum extends AbstractSourceBuilder<HandlerContext> {

	@Override
	protected void build() {
		XmlEnum xml = ctx.xml();
		TypeModel t = xml.type();

		cl.addImplementedType(types.getClass(XmlSimpleHandler.class, types.getClass(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, cl.getNameAsString()), "INSTANCE",
				new ObjectCreationExpr(null, new ClassOrInterfaceType(null, cl.getNameAsString()), Utils.list()), Utils.PUBLIC_STATIC);

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();

		QName qname = JaxbGeneratorMojo.getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.getClass(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(new ObjectCreationExpr(null, types.getClass(QName.class),
					Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		NodeList<SwitchEntry> list = xml.entries().stream()
				.map(e -> new SwitchEntry().setLabels(Utils.list(new NameExpr(e.name()))).addStatement(new ReturnStmt(new StringLiteralExpr(e.value()))))
				.collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(
				new ThrowStmt(new ObjectCreationExpr(null, types.getClass(JAXBException.class), Utils.list(new StringLiteralExpr("Unsupported enum constant"))))));
		cl.addMethod("toString", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(JAXBException.class))
				.addParameter(types.get(xml.type()), "o").setType(types.get(String.class)).createBody().addStatement(new SwitchStmt(new NameExpr("o"), list));

		list = xml.entries().stream().map(e -> new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(e.value())))
				.addStatement(new ReturnStmt(new FieldAccessExpr(new TypeExpr(types.get(t)), e.name())))).collect(Collectors.toCollection(() -> Utils.list()));
		list.add(new SwitchEntry().addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(JAXBException.class),
				Utils.list(new BinaryExpr(new StringLiteralExpr("Unsupported enum value "), new NameExpr("s"), BinaryExpr.Operator.PLUS))))));
		cl.addMethod("toObject", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(JAXBException.class))
				.addParameter(types.get(String.class), "s").setType(types.get(xml.type())).createBody().addStatement(new SwitchStmt(new NameExpr("s"), list));

	}

}
