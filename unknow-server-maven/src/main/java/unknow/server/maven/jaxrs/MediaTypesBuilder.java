/**
 * 
 */
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
import unknow.server.http.jaxrs.MTPredicate;
import unknow.server.maven.Output;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;

/**
 * @author unknow
 */
public class MediaTypesBuilder {
	private final CompilationUnit cu;
	private final ClassOrInterfaceDeclaration cl;
	private final TypeCache types;

	private final ClassOrInterfaceType mt;

	private final Map<String, Expression> mts;
	private final Map<String, Expression> predicates;

	public MediaTypesBuilder(CompilationUnit cu, Map<String, String> existingClass) {
		this.cu = cu;
		types = new TypeCache(cu, existingClass);
		cl = cu.addClass("MediaTypes", Utils.PUBLIC);

		mt = types.getClass(MediaType.class);

		mts = new HashMap<>();
		predicates = new HashMap<>();

		mts.put("application/xml", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_XML_TYPE"));
		mts.put("application/atom+xml", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_ATOM_XML_TYPE"));
		mts.put("application/xhtml+xml", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_XHTML_XML_TYPE"));
		mts.put("application/svg+xml", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_SVG_XML_TYPE"));
		mts.put("application/json", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_JSON_TYPE"));
		mts.put("application/x-www-form-urlencoded", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_FORM_URLENCODED_TYPE"));
		mts.put("multipart/form-data", new FieldAccessExpr(new TypeExpr(mt), "MULTIPART_FORM_DATA_TYPE"));
		mts.put("application/octet-stream", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_OCTET_STREAM_TYPE"));
		mts.put("text/plain", new FieldAccessExpr(new TypeExpr(mt), "TEXT_PLAIN_TYPE"));
		mts.put("text/xml", new FieldAccessExpr(new TypeExpr(mt), "TEXT_XML_TYPE"));
		mts.put("text/html", new FieldAccessExpr(new TypeExpr(mt), "TEXT_HTML_TYPE"));
		mts.put("text/event-stream", new FieldAccessExpr(new TypeExpr(mt), "SERVER_SENT_EVENTS_TYPE"));
		mts.put("application/json-patch+json", new FieldAccessExpr(new TypeExpr(mt), "APPLICATION_JSON_PATCH_JSON_TYPE"));
	}

	public void save(Output out) throws MojoExecutionException {
		if (!predicates.isEmpty() || mts.size() > 13)
			out.save(cu);
	}

	public Expression type(String t) {
		Expression n = mts.get(t);
		if (n != null)
			return n;
		String[] split = t.split("/");
		String name = t.toUpperCase().replaceAll("[^_a-zA-Z]", "_");

		cl.addFieldWithInitializer(mt, name,
				new ObjectCreationExpr(null, types.getClass(MediaType.class), Utils.list(new StringLiteralExpr(split[0]), new StringLiteralExpr(split[1]))),
				Utils.PUBLIC_STATIC);
		mts.put(t, n = new FieldAccessExpr(new TypeExpr(types.getClass(cl)), name));
		return n;
	}

	public Expression predicate(Collection<String> mediaTypes) {
		String k = "";
		if (!mediaTypes.contains("*/*")) {
			List<String> l = new ArrayList<>(mediaTypes);
			l.sort(null);
			StringJoiner s = new StringJoiner(",");
			for (String str : l) {
				if (!"*/*".equals(str))
					s.add(str);
			}
			k = s.toString();
		}

		Expression n = predicates.get(k);
		if (n != null)
			return n;

		String name = "p$" + predicates.size();

		Expression e = null;
		if (!mediaTypes.contains("*/*")) {
			NodeList<Expression> l = new NodeList<>();
			for (String s : mediaTypes)
				l.add(type(s));
			e = new ObjectCreationExpr(null, types.getClass(MTPredicate.OneOf.class), l);
		} else
			e = new FieldAccessExpr(new TypeExpr(types.get(MTPredicate.class)), "ANY");

		cl.addFieldWithInitializer(types.getClass(MTPredicate.class), name, e, Utils.PUBLIC_STATIC);
		predicates.put(k, n = new FieldAccessExpr(new TypeExpr(types.getClass(cl)), name));
		return n;
	}
}
