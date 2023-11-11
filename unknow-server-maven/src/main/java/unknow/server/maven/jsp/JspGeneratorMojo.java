package unknow.server.maven.jsp;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import unknow.server.http.servlet.ServletJsp;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;

/**
 * @author unknow
 */
public class JspGeneratorMojo {
	private static final NameExpr OUT = new NameExpr("out");

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(name = "src", defaultValue = "${project.build.sourceDirectory}")
	private String src;

	@Parameter(name = "className", defaultValue = "Server")
	private String className;

	@Parameter(name = "packageName")
	private String packageName;

	@Parameter(name = "output")
	private String output;

	private final StringBuilder print = new StringBuilder();
	private final StringBuilder sb = new StringBuilder();

	private TypeCache types;

	private BlockStmt b;

	public void generate(Path file) throws IOException {
		CompilationUnit cu = new CompilationUnit("jsp");
		ClassOrInterfaceDeclaration cl = cu.addClass("Impl", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addExtendedType(ServletJsp.class);
		types = new TypeCache(cu, Collections.emptyMap());

		b = cl.addMethod("jspService", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).addParameter(types.getClass(HttpServletRequest.class), "request")
				.addParameter(types.getClass(HttpServletResponse.class), "response").addMarkerAnnotation(Override.class).createBody();
		try (Reader r = Files.newBufferedReader(file)) {
			parse(r);
		}
		System.out.println(cu);
	}

	private void parse(Reader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '<') {
				sb.setLength(0);
				processTag(r);
			} else
				print.append((char) c);
		}
		print();
	}

	/**
	 * add out.print()
	 */
	private void print() {
		if (print.length() == 0)
			return;
		b.addStatement(new MethodCallExpr(OUT, "print", Utils.list(Utils.text(print.toString()))));
		print.setLength(0);
	}

	/**
	 * process a tag start
	 * 
	 * @param r
	 * @throws IOException
	 */
	private void processTag(Reader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '>') {
				print.append('<').append(sb).append('>');
				sb.setLength(0);
				return;
			}
			if (c == ' ') {
				String tag = sb.toString();
				sb.setLength(0);
				if ("%!--".equals(tag)) {
					processComment(r);
					return;
				}
				if ("%@".equals(tag)) {
					print();
					processDirective(r);
					return;
				}
				if ("%=".equals(tag)) {
					print();
					processEcho(r);
					return;
				}
				if ("%".equals(tag)) {
					print();
					processScript(r);
					return;
				}
				// TODO action jsp:include, jsp:forward
				// TODO taglib
				print.append('<').append(tag).append(' ');
				while ((c = r.read()) != -1) {
					print.append((char) c);
					if (c == '>')
						return;
				}
			} else
				sb.append((char) c);
		}
		print.append(sb);
		sb.setLength(0);
	}

	private void processComment(Reader r) throws IOException {
		int c;

	}

	/**
	 * process <%@ content
	 * 
	 * @param r
	 * @throws IOException
	 */
	private void processDirective(Reader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '%') {
				c = r.read();
				if (c == '>')
					break;
				sb.append('%');
				if (c == -1)
					break;
			}
			sb.append((char) c);
		}
		System.out.println("@: " + sb);
		sb.setLength(0);
	}

	/**
	 * process <%= content
	 * 
	 * @param r
	 * @throws IOException
	 */
	private void processEcho(Reader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '%') {
				c = r.read();
				if (c == '>')
					return;
				sb.append('%');
				if (c == -1)
					break;
			}
			sb.append((char) c);
		}
		b.addStatement(new MethodCallExpr(OUT, "print", Utils.list(StaticJavaParser.parseExpression(sb.toString()))));
		sb.setLength(0);
	}

	/**
	 * process <% content
	 * 
	 * @param r
	 * @throws IOException
	 */
	private void processScript(Reader r) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '%') {
				c = r.read();
				if (c == '>') {
					sb.insert(0, '{').append('}');
					System.out.println(sb);
					BlockStmt parseBlock = StaticJavaParser.parseBlock(sb.toString());
					for (Statement s : parseBlock.getStatements())
						b.addStatement(s);
					sb.setLength(0);
					return;
				}
				sb.append('%');
				if (c == -1)
					break;
			}
			sb.append((char) c);
		}
		print.append(sb);
		sb.setLength(0);
	}

	public static void main(String[] a) throws IOException {
		new JspGeneratorMojo().generate(Paths.get("../unknow-http-test/src/main/resources/echo.jsp"));
	}
}