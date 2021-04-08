/**
 * 
 */
package unknow.server.maven.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

/**
 * @author unknow
 */
public class Descriptor implements Consumer<CompilationUnit> {
	public static final List<Class<?>> LISTENERS = Arrays.asList(ServletContextListener.class, ServletContextAttributeListener.class, ServletRequestListener.class, ServletRequestAttributeListener.class, HttpSessionListener.class, HttpSessionAttributeListener.class, HttpSessionIdListener.class);

	public String name = "/";

	public final Map<String, String> param = new HashMap<>();

	public final List<LD> listeners = new ArrayList<>();
	public final List<SD> servlets = new ArrayList<>();
	public final List<SD> filters = new ArrayList<>();

	public final Map<String, String> errorClass = new HashMap<>();
	public final Map<Integer, String> errorCode = new HashMap<>();

	@Override
	public void accept(CompilationUnit c) {
		for (ClassOrInterfaceDeclaration t : c.findAll(ClassOrInterfaceDeclaration.class)) {
			Optional<AnnotationExpr> o = t.getAnnotationByClass(WebServlet.class);
			if (o.isPresent()) {
				SD sd = new SD(servlets.size(), o.get(), t);
				servlets.add(sd);
			}
			o = t.getAnnotationByClass(WebFilter.class);
			if (o.isPresent()) {
				SD sd = new SD(filters.size(), o.get(), t);
				filters.add(sd);
			}
			o = t.getAnnotationByClass(WebListener.class);
			if (o.isPresent())
				processListener(t);
		}
	}

	private void processListener(ClassOrInterfaceDeclaration t) {
		Set<String> ancestror = new HashSet<>();
		for (ResolvedReferenceType i : t.resolve().getAllAncestors())
			ancestror.add(i.getQualifiedName());

		Set<Class<?>> listener = new HashSet<>();
		for (Class<?> cl : LISTENERS) {
			if (ancestror.contains(cl.getName()))
				listener.add(cl);
		}

		listeners.add(new LD(t.resolve().getQualifiedName(), listener));
	}

	public List<SD> findFilters(String path, DispatcherType type) {
		List<SD> matching = new ArrayList<>();
		for (SD f : filters) {
			if (!f.dispatcher.contains(type))
				continue;
			for (String p : f.pattern) {
				if (p.endsWith("/*") && path.startsWith(p.substring(0, p.length() - 2))) {
					matching.add(f);
					break;
				} else if (p.startsWith("*") && path.endsWith(p.substring(1))) {
					matching.add(f);
					break;
				} else if (path.equals(p)) {
					matching.add(f);
					break;
				}
			}
		}
		return matching;
	}

	public SD findServlet(String path) {
		int l = 0;
		SD best = null;
		for (SD s : servlets) {
			for (String p : s.pattern) {
				if (p.endsWith("/*") && path.startsWith(p.substring(0, p.length() - 2)) && l < p.length() - 2) {
					best = s;
					l = p.length() - 2;
				} else if (p.startsWith("*") && path.endsWith(p.substring(1)) && l == 0)
					best = s;
				else if (path.equals(p))
					return s;
			}
		}
		return best;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name:\t").append(name).append("\n");
		sb.append("servlets:\t").append(servlets).append("\n");
		sb.append("filters:\t").append(filters).append("\n");
		sb.append("errorClass:\t").append(errorClass).append("\n");
		sb.append("errorCodes:\t").append(errorCode).append("\n");
		return sb.toString();
	}
}
