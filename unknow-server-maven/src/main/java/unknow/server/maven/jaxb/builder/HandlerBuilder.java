package unknow.server.maven.jaxb.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import unknow.server.maven.SourceBuilder;
import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxb.HandlerContext;
import unknow.server.maven.jaxb.model.XmlEnum;
import unknow.server.maven.jaxb.model.XmlTypeComplex;

public class HandlerBuilder implements SourceBuilder<HandlerContext> {
	private static final Logger logger = LoggerFactory.getLogger(HandlerBuilder.class);

	private static final HandlerEnum ENUM = new HandlerEnum();
	private static final HandlerObject OBJECT = new HandlerObject();

	@Override
	public void process(ClassOrInterfaceDeclaration cl, TypeCache types, HandlerContext ctx) {
		if (ctx.xml() instanceof XmlEnum)
			ENUM.process(cl, types, ctx);
		else if (ctx.xml() instanceof XmlTypeComplex)
			OBJECT.process(cl, types, ctx);
		else
			logger.warn(">> " + ctx.xml());
	}
}
