package unknow.server.maven.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.maven.plugin.MojoExecutionException;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import jakarta.ws.rs.core.MediaType;
import unknow.maven.codegen.CodeGenUtils;
import unknow.maven.codegen.CompilationUnitWriter;
import unknow.maven.codegen.TypeFactory;
import unknow.server.http.jaxrs.MTPredicate;

/**
 * Build MediaTypes class with added MTPredicate and MediaType
 * @author unknow
 */
public class MediaTypesBuilder {
	private static final Map<String, String> DEFAULT = new HashMap<>();

	static {
		DEFAULT.put("application/xml", "APPLICATION_XML_TYPE");
		DEFAULT.put("application/atom+xml", "APPLICATION_ATOM_XML_TYPE");
		DEFAULT.put("application/xhtml+xml", "APPLICATION_XHTML_XML_TYPE");
		DEFAULT.put("application/svg+xml", "APPLICATION_SVG_XML_TYPE");
		DEFAULT.put("application/json", "APPLICATION_JSON_TYPE");
		DEFAULT.put("application/x-www-form-urlencoded", "APPLICATION_FORM_URLENCODED_TYPE");
		DEFAULT.put("multipart/form-data", "MULTIPART_FORM_DATA_TYPE");
		DEFAULT.put("application/octet-stream", "APPLICATION_OCTET_STREAM_TYPE");
		DEFAULT.put("text/plain", "TEXT_PLAIN_TYPE");
		DEFAULT.put("text/xml", "TEXT_XML_TYPE");
		DEFAULT.put("text/html", "TEXT_HTML_TYPE");
		DEFAULT.put("text/event-stream", "SERVER_SENT_EVENTS_TYPE");
		DEFAULT.put("application/json-patch+json", "APPLICATION_JSON_PATCH_JSON_TYPE");
	}

	private final CompilationUnit cu;
	private final ClassOrInterfaceDeclaration cl;
	private final TypeFactory types;

	private final ClassOrInterfaceType mt;

	private final Map<String, Expression> mts;
	private final Map<String, Expression> predicates;

	public MediaTypesBuilder(CompilationUnit cu, Map<String, String> existingClass) {
		this.cu = cu;
		types = new TypeFactory(cu, existingClass);
		cl = cu.addClass("MediaTypes", CodeGenUtils.PUBLIC);

		mt = types.getClass(MediaType.class);

		mts = new HashMap<>();
		predicates = new HashMap<>();

	}

	/**
	 * write the MediaTypes class if needed
	 * @param writer writer to write to
	 * @throws MojoExecutionException in case od error
	 */
	public void save(CompilationUnitWriter writer) throws MojoExecutionException {
		if (!predicates.isEmpty() || mts.size() > 13)
			writer.write(cu);
	}

	/**
	 * get an exception to an MediaType
	 * @param f factory to add the required import
	 * @param t mediaType
	 * @return the exception
	 */
	public Expression type(TypeFactory f, String t) {
		String field = DEFAULT.get(t);
		if (field != null)
			return new FieldAccessExpr(new TypeExpr(f.getClass(MediaType.class)), field);

		Expression n = mts.get(t);
		if (n != null)
			return n;

		String[] split = t.split("/");
		String name = t.toUpperCase().replaceAll("[^_a-zA-Z]", "_");

		cl.addFieldWithInitializer(mt, name,
				new ObjectCreationExpr(null, types.getClass(MediaType.class), CodeGenUtils.list(new StringLiteralExpr(split[0]), new StringLiteralExpr(split[1]))),
				CodeGenUtils.PUBLIC_STATIC);
		mts.put(t, n = new FieldAccessExpr(new TypeExpr(types.getClass(cl)), name));
		return n;
	}

	/**
	 * get an exception to an MTPredicate
	 * @param f factory to add the required import
	 * @param mediaTypes types accepted by the predicate
	 * @return the exception
	 */
	public Expression predicate(TypeFactory f, Collection<String> mediaTypes) {
		String k = "";
		if (!mediaTypes.contains("*/*")) {
			List<String> l = new ArrayList<>(mediaTypes);
			l.sort(null);
			StringJoiner s = new StringJoiner(",");
			for (String str : l)
				s.add(str);
			k = s.toString();
		}

		Expression n = predicates.get(k);
		if (n != null)
			return n;

		String name = "p$" + predicates.size();

		Expression e = null;
		if (mediaTypes.contains("*/*"))
			e = new FieldAccessExpr(new TypeExpr(types.get(MTPredicate.class)), "ANY");
		else if (mediaTypes.size() == 1)
			e = new ObjectCreationExpr(null, types.getClass(MTPredicate.Single.class), CodeGenUtils.list(type(types, mediaTypes.iterator().next())));
		else {
			NodeList<Expression> l = new NodeList<>();
			for (String s : mediaTypes)
				l.add(type(types, s));
			e = new ObjectCreationExpr(null, types.getClass(MTPredicate.OneOf.class), l);
		}

		cl.addFieldWithInitializer(types.getClass(MTPredicate.class), name, e, CodeGenUtils.PUBLIC_STATIC);
		predicates.put(k, n = new FieldAccessExpr(new TypeExpr(f.getClass(cl)), name));
		return n;
	}
}
