/**
 * 
 */
package unknow.server.maven.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServlet;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

/**
 * @author unknow
 */
public class JspServletGenerator {
	public static void generate(Path file) throws IOException {
		CompilationUnit cu = new CompilationUnit("jsp");
		ClassOrInterfaceDeclaration cl = cu.addClass("Impl", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addExtendedType(HttpServlet.class);

		cl.addMethod("service", null);
		try (BufferedReader r = Files.newBufferedReader(file)) {
			parse(r);
		}
	}

	public static void parse(Reader r) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;
		while ((c = r.read()) != -1) {
			if (c == '<') {
				c = r.read();
				if (c == -1) {
					sb.append('<');
					break;
				} else if (c == '%') { // <%
					System.out.println(sb);
					sb.setLength(0);
					readScript(r, sb);
				} else
					sb.append('<').append((char) c);
			} else
				sb.append((char) c);
		}
		System.out.println(sb);
	}

	/**
	 * @param r
	 * @throws IOException
	 */
	private static void readScript(Reader r, StringBuilder sb) throws IOException {
		// ! declaration
		// = out.print
		// -- comment
		// @ directive
		int start = r.read();
		int c;
		while ((c = r.read()) != -1) {
			if (c == '%') {
				c = r.read();
				if (c == -1)
					throw new IOException("<%" + (char) start + " unterminated");
				if (c == '>') {
					System.out.println("--");
					System.out.println(sb);
					sb.setLength(0);
					System.out.println("--");
					return;
				}
				sb.append('%').append((char) c);
			} else
				sb.append((char) c);
		}
		throw new IOException("<%" + (char) start + " unterminated");
	}

	public static void main(String[] a) throws IOException {
		parse(new StringReader("<html><body><h1><%= \"out\"%></h1></html>"));
	}
}