package unknow.server.maven.jaxb.builder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import unknow.server.jaxb.XmlHandler;
import unknow.server.jaxb.XmlRootHandler;
import unknow.server.maven.SourceBuilder.AbstractSourceBuilder;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxb.HandlerContext;
import unknow.server.maven.jaxb.JaxbGeneratorMojo;
import unknow.server.maven.jaxb.model.XmlChoice;
import unknow.server.maven.jaxb.model.XmlCollection;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.jaxb.model.XmlTypeComplex;
import unknow.server.maven.model.TypeModel;

public class HandlerObject extends AbstractSourceBuilder<HandlerContext> {
	private static final List<AbstractSourceBuilder<HandlerContext>> methods = Arrays.asList(new HandlerObjectWriter(), new HandlerObjectReader());

	@Override
	protected void build() {
		XmlTypeComplex xml = ctx.xml();
		TypeModel t = ctx.type();

		cl.addImplementedType(types.getClass(XmlHandler.class, types.getClass(t)));

		cl.addFieldWithInitializer(new ClassOrInterfaceType(null, cl.getNameAsString()), "INSTANCE",
				new ObjectCreationExpr(null, new ClassOrInterfaceType(null, cl.getNameAsString()), Utils.list()), Utils.PUBLIC_STATIC);

		AtomicInteger i = new AtomicInteger(0);
		for (XmlElement e : xml.getAttributes())
			cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$a" + i.getAndIncrement(),
					new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);

		i.set(0);
		for (XmlElement e : xml.getElements())
			appendInitElement(cl, types, e, i);

		BlockStmt b = cl.addConstructor(Modifier.Keyword.PRIVATE).getBody();
		QName qname = JaxbGeneratorMojo.getRootQN(t);
		if (qname != null) {
			cl.addExtendedType(types.getClass(XmlRootHandler.class, types.getClass(t)));
			b.addStatement(new MethodCallExpr(null, "super", Utils.list(new ObjectCreationExpr(null, types.getClass(QName.class),
					Utils.list(new StringLiteralExpr(qname.getNamespaceURI()), new StringLiteralExpr(qname.getLocalPart()))))));
		}

		for (AbstractSourceBuilder<HandlerContext> m : methods)
			m.process(cl, types, ctx);
	}

	private void appendInitElement(ClassOrInterfaceDeclaration cl, TypeCache types, XmlElement e, AtomicInteger i) {
		XmlType x = e.xmlType();
		if (x instanceof XmlCollection)
			x = ((XmlCollection) x).component();

		if (x instanceof XmlChoice) {
			for (XmlElement v : ((XmlChoice) x).choice())
				appendInitElement(cl, types, v, i);
			return;
		}

		cl.addFieldWithInitializer(types.get(QName.class), e.name() + "$e" + i.getAndIncrement(),
				new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(new StringLiteralExpr(e.ns()), new StringLiteralExpr(e.name()))), Utils.PSF);

		if (e.xmlType() instanceof XmlCollection && e.type().isArray())
			ctx.emptyArray(e.type(), name -> cl.addFieldWithInitializer(types.get(e.type()), name, new ArrayInitializerExpr(), Utils.PSF));
	}
}
