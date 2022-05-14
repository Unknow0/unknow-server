///**
// * 
// */
package unknow.server.maven.jsp;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.Reader;
//import java.io.StringReader;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//import javax.servlet.http.HttpServlet;
//
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.CompilationUnit;
//import com.github.javaparser.ast.Modifier;
//import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
//import com.github.javaparser.ast.body.MethodDeclaration;
//import com.github.javaparser.ast.expr.MethodCallExpr;
//import com.github.javaparser.ast.expr.NameExpr;
//import com.github.javaparser.ast.expr.StringLiteralExpr;
//import com.github.javaparser.ast.stmt.BlockStmt;
//
//import unknow.server.maven.Utils;
//
///**
// * @author unknow
// */
public class JspServletGenerator {
//	private static final NameExpr OUT = new NameExpr("out");
//
//	public static void generate(Reader r) throws IOException {
//		CompilationUnit cu = new CompilationUnit("jsp");
//		ClassOrInterfaceDeclaration cl = cu.addClass("Impl", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addExtendedType(HttpServlet.class);
//
//		BlockStmt b = cl.addMethod("service", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).getBody().get();
//		parse(r, b);
//		System.out.println(cu);
//	}
//
//	public static void parse(Reader r, BlockStmt b) throws IOException {
//		StringBuilder sb = new StringBuilder();
//		int c;
//		while ((c = r.read()) != -1) {
//			if (c == '<') {
//				processTag(r, sb);
//				c = r.read();
//				if (c == -1) {
//					sb.append('<');
//					break;
//				}
//
//			} else
//				sb.append((char) c);
//		}
//		System.out.println(sb);
//	}
//
//	private static void processTag(Reader r, StringBuilder sb) throws IOException {
//		int c;
//		while ((c = r.read()) != -1) {
//			if (c == '>') {
//				sb.insert(0, '<').append('>');
//				break;
//			}
//			if (c == ' ') {
//				String tag = sb.toString();
//				sb.setLength(0);
//				if ("%=".equals(tag)) {
////					b.addStatement(new MethodCallExpr(OUT, "print", Utils.list(StaticJavaParser.parseExpression(sb.toString()))));
//				}
//			} else
//				sb.append((char) c);
//		}
//	}
//
//	/**
//	 * @param r
//	 * @param b
//	 * @throws IOException
//	 */
//	private static void readScript(Reader r, StringBuilder sb, BlockStmt b) throws IOException {
//		// ! declaration
//		// = out.print
//		// -- comment
//		// @ directive
//		int start = r.read();
//		int c = r.read();
//		while ((c = r.read()) != -1) {
//			if (c == '%') {
//				c = r.read();
//				if (c == -1)
//					throw new IOException("<%" + (char) start + " unterminated");
//				if (c == '>') {
//					System.out.println("--");
//					System.out.println(StaticJavaParser.parseExpression(sb.toString()));
//					sb.setLength(0);
//					System.out.println("--");
//					return;
//				}
//				sb.append('%').append((char) c);
//			} else
//				sb.append((char) c);
//		}
//		throw new IOException("<%" + (char) start + " unterminated");
//	}
//
//	public static void main(String[] a) throws IOException {
//		generate(new StringReader("<html><body><h1><%= \"out\"%></h1></html>"));
//	}
}