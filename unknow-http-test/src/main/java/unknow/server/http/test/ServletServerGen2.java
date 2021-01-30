//package unknow.server.maven;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.Writer;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import javax.annotation.Generated;
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.Filer;
//import javax.annotation.processing.ProcessingEnvironment;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedSourceVersion;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.ElementKind;
//import javax.lang.model.element.Modifier;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.type.TypeMirror;
//import javax.lang.model.util.Elements;
//import javax.lang.model.util.Types;
//import javax.servlet.Servlet;
//import javax.servlet.ServletContext;
//import javax.servlet.ServletContextAttributeListener;
//import javax.servlet.ServletContextListener;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
//import javax.servlet.annotation.WebInitParam;
//import javax.servlet.annotation.WebListener;
//import javax.servlet.annotation.WebServlet;
//
//import com.squareup.javapoet.AnnotationSpec;
//import com.squareup.javapoet.CodeBlock;
//import com.squareup.javapoet.FieldSpec;
//import com.squareup.javapoet.JavaFile;
//import com.squareup.javapoet.MethodSpec;
//import com.squareup.javapoet.ParameterizedTypeName;
//import com.squareup.javapoet.TypeSpec;
//
//import picocli.CommandLine;
//import unknow.server.http.HttpHandler;
//import unknow.server.http.HttpRawProcessor;
//import unknow.server.http.HttpRawRequest;
//import unknow.server.http.servlet.ArrayMap;
//import unknow.server.http.servlet.ServletConfigImpl;
//import unknow.server.http.servlet.ServletContextImpl;
//import unknow.server.http.servlet.ServletRequestImpl;
//import unknow.server.nio.Handler;
//import unknow.server.nio.HandlerFactory;
//import unknow.server.nio.cli.NIOServerCli;
//import unknow.server.nio.util.Buffers;
//import unknow.server.nio.util.BuffersUtils;
//
///**
// * @author unknow
// */
//
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
//public class ServletServerGen2 extends AbstractProcessor {
//	private static final Set<String> ANNOTATIONS = new HashSet<>(Arrays.asList(WebServlet.class.getName(), WebListener.class.getName(), WebFilter.class.getName()));
//
//	private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class).addMember("value", "$S", "ServletServerGen").build();
//
//	private static final byte[] NOT_FOUND = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
//	private static final byte[] ERROR = "HTTP/1.1 500 Server Error\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
//
//	private String packageName = "unknow.server.http.test"; // get from option ?
//
//	private Elements elements;
//	private Types types;
//	private Filer filer;
//
//	private TypeMirror servletContextListener;
//	private TypeMirror servletContextAttributeListener;
//
//	private List<SData> servlet = new ArrayList<>();
//	private List<Element> ctxListeners = new ArrayList<>();
//	private List<Element> ctxAttrListeners = new ArrayList<>();
//
//	@Override
//	public Set<String> getSupportedAnnotationTypes() {
//		return ANNOTATIONS;
//	}
//
//	@Override
//	public synchronized void init(ProcessingEnvironment processingEnv) {
//		super.init(processingEnv);
//		elements = processingEnv.getElementUtils();
//		types = processingEnv.getTypeUtils();
//		filer = processingEnv.getFiler();
//
//		servletContextListener = elements.getTypeElement(ServletContextListener.class.getCanonicalName()).asType();
//		servletContextAttributeListener = elements.getTypeElement(ServletContextAttributeListener.class.getCanonicalName()).asType();
//	}
//
//	@Override
//	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//		collect(roundEnv, servlet);
//		collectListener(roundEnv, ctxListeners, ctxAttrListeners);
//
//		String name = "/"; // TODO get from web.xml
//
//		List<FieldSpec> fields = new ArrayList<>();
//		fields.add(FieldSpec.builder(byte[].class, "NOT_FOUND", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).initializer(byteArray(NOT_FOUND)).build());
//		fields.add(FieldSpec.builder(byte[].class, "ERROR", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).initializer(byteArray(ERROR)).build());
//
//		fields.add(FieldSpec.builder(ServletContext.class, "CTX", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).build());
//
//		CodeBlock.Builder init = CodeBlock.builder();
//		init.addStatement("$1T<String> initParam=new $1T<>()", ArrayMap.class);
//		// TODO get initParam & name from web.xml
//
//		init.addStatement("$T listeners=new $T<>()", ParameterizedTypeName.get(List.class, ServletContextAttributeListener.class), ArrayList.class);
//		for (Element e : ctxAttrListeners)
//			init.addStatement("listeners.add(new $T())", e);
//
//		init.addStatement("CTX=new $T($S, initParam, listeners)", ServletContextImpl.class, name);
//
//		// create an initialize servlet in loadOnStartup order
//		Collections.sort(servlet, (a, b) -> a.a.loadOnStartup() - b.a.loadOnStartup());
//		init.beginControlFlow("try");
//		for (SData s : servlet) {
//			WebServlet a = s.a;
//			fields.add(FieldSpec.builder(Servlet.class, s.name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("new $T()", s.e).build());
//			init.addStatement("initParam=new $T<>()", ArrayMap.class);
//			WebInitParam[] initParams = a.initParams();
//			for (int i = 0; i < initParams.length; i++)
//				init.addStatement("initParam.set($S,$S)", initParams[i].name(), initParams[i].value());
//			init.addStatement("$L.init(new $T($S, CTX, initParam))", s.name, ServletConfigImpl.class, a.displayName());
//		}
//		init.nextControlFlow("catch($T e)", ServletException.class);
//		init.addStatement("throw new $T(e)", RuntimeException.class);
//		init.endControlFlow();
//
//		// TODO add code for discovering & running ServletContainerInitializer
//		// ServletContainerInitializer modify context at runtime :s
//
//		List<MethodSpec> m = new ArrayList<>();
//		m.add(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
//		m.add(generateProcess());
//		m.add(generateFindServlet(fields));
//		m.add(generateCall());
//		m.add(generateMain("Server"));
//
//		TypeSpec.Builder server = TypeSpec.classBuilder("Server").addModifiers(Modifier.PUBLIC, Modifier.FINAL).superclass(NIOServerCli.class).addSuperinterface(HttpRawProcessor.class);
//		server.addAnnotation(GENERATED).addStaticBlock(init.build()).addFields(fields).addMethods(m);
//
//		writeClass(server.build());
//		return true;
//	}
//
//	private void writeClass(TypeSpec processor) {
//		JavaFile build = JavaFile.builder(packageName, processor).indent("	").skipJavaLangImports(true).build();
//
//		try (Writer w = filer.createSourceFile(packageName + "." + processor.name).openWriter()) {
//			build.writeTo(w);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void collect(RoundEnvironment roundEnv, List<SData> list) {
//		int i = 0;
//		for (Element e : roundEnv.getElementsAnnotatedWith(WebServlet.class)) {
//			list.add(new SData("S" + i + "$", e.getAnnotation(WebServlet.class), e));
//		}
//	}
//
//	private void collectListener(RoundEnvironment roundEnv, List<Element> ctxListeners, List<Element> ctxAttrListeners) {
//		for (Element e : roundEnv.getElementsAnnotatedWith(WebListener.class)) {
//			if (e.getKind() != ElementKind.CLASS)
//				continue;
//			TypeMirror asType = e.asType();
//			if (types.isAssignable(asType, servletContextListener))
//				ctxListeners.add(e);
//			else if (types.isAssignable(asType, servletContextAttributeListener))
//				ctxAttrListeners.add(e);
//
////			 * {@link javax.servlet.ServletRequestListener},
////			 * {@link javax.servlet.ServletRequestAttributeListener}, 
////			 * {@link javax.servlet.http.HttpSessionListener}, or
////			 * {@link javax.servlet.http.HttpSessionAttributeListener}, or
////			 * {@link javax.servlet.http.HttpSessionIdListener} interfaces.
////			init.add("new $T().
//		}
//	}
//
//	private static MethodSpec generateCall() {
//		MethodSpec.Builder m = MethodSpec.methodBuilder("call").addModifiers(Modifier.PUBLIC).returns(Integer.class).addAnnotation(Override.class).addException(Exception.class);
//		m.addStatement("CTX.set(address, port)");
//
//		m.addStatement("return super.call()");
//		return m.build();
//	}
//
//	private static MethodSpec generateMain(String cl) {
//		MethodSpec.Builder m = MethodSpec.methodBuilder("main").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
//		m.addParameter(String[].class, "arg");
//
//		m.addStatement("$1L c=new $1L()", cl);
//		m.addStatement("$T executor=$T.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t; })", ExecutorService.class, Executors.class);
//		m.beginControlFlow("c.handler=new $T()", HandlerFactory.class);
//		m.beginControlFlow("@Override protected $T create()", Handler.class);
//		m.addStatement("return new $T(executor, c)", HttpHandler.class);
//		m.endControlFlow().endControlFlow(";");
//		m.addStatement("System.exit(new $T(c).execute(arg))", CommandLine.class);
//
//		return m.build();
//	}
//
//	private static MethodSpec generateProcess() {
////		void process(HttpRawRequest request, OutputStream out) throws IOException;
//
//		MethodSpec.Builder m = MethodSpec.methodBuilder("process").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
//		m.addAnnotation(Override.class).addException(IOException.class);
//		m.addParameter(HttpRawRequest.class, "request").addParameter(OutputStream.class, "out");
//
//		m.addStatement("$T s=findServlet(request)", Servlet.class);
//		m.beginControlFlow("if(s==null)");
//		m.addStatement("out.write(NOT_FOUND)");
//		m.addStatement("out.close()");
//		m.addStatement("return");
//		m.endControlFlow();
//
//		// create http request & response;
//		m.addStatement("$T req=new $T(CTX,request)", ServletRequest.class, ServletRequestImpl.class);
//		m.addStatement("$T res=null", ServletResponse.class);
//
//		m.beginControlFlow("try");
//		m.addStatement("s.service(req, res)");
//		m.nextControlFlow("catch($T e)", Exception.class);
//		// TODO log
//		m.addStatement("out.write(ERROR)");
//		m.endControlFlow();
//		m.addStatement("out.close()");
//		return m.build();
//	}
//
//	private MethodSpec generateFindServlet(List<FieldSpec> fields) {
//		Map<String, SData> map = new HashMap<>();
//		List<String> path = new ArrayList<>();
//		SData def = null;
//		for (SData s : servlet) {
//			WebServlet a = s.a;
//			String[] value = a.value();
//			for (int i = 0; i < value.length; i++) {
//				String p = value[i];
//				if (!p.equals("/")) {
//					map.put(p, s);
//					path.add(p);
//				} else
//					def = s;
//			}
//			value = a.urlPatterns();
//			for (int i = 0; i < value.length; i++) {
//				String p = value[i];
//				if (!p.equals("/")) {
//					map.put(p, s);
//					path.add(p);
//				} else
//					def = s;
//			}
//		}
//
//		// TODO precompile Filter
//
//		MethodSpec.Builder m = MethodSpec.methodBuilder("findServlet").returns(Servlet.class).addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);
//		m.addParameter(HttpRawRequest.class, "request");
//		m.addStatement("$T path=request.path", Buffers.class);
//		Collections.sort(path, (a, b) -> a.length() - b.length());
//		int pi = 0;
//		for (String p : path) {
//			String me;
//			String name = map.get(p).name;
//			if (p.charAt(0) == '/' && p.endsWith("/*")) {
//				me = "pathMatches";
//				p = p.substring(0, p.length() - 2);
//			} else if (p.startsWith("*.")) {
//				me = "endsWith";
//				p = p.substring(1);
//			} else
//				me = "equals";
//
//			String n = "P" + pi + "$";
//			fields.add(FieldSpec.builder(byte[].class, n, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer(byteArray(p.getBytes(StandardCharsets.UTF_8))).build());
//			m.addStatement("if($T.$L(path, $L)) return $L", BuffersUtils.class, me, n, name);
//		}
//
//		// find & run servlet
//		return m.addStatement("return $L", def == null ? "null" : def.name).build();
//	}
//
//	private static CodeBlock byteArray(byte[] b) {
//		CodeBlock.Builder c = CodeBlock.builder();
//		c.add("new byte[]{");
//		if (b.length > 0) {
//			c.add("0x").add(Integer.toHexString(b[0]));
//			for (int i = 1; i < b.length; i++)
//				c.add(",0x").add(Integer.toHexString(b[i]));
//		}
//		c.add("}");
//		return c.build();
//	}
//
//	private static class SData {
//		final String name;
//		final WebServlet a;
//		final Element e;
//
//		public SData(String name, WebServlet a, Element e) {
//			this.name = name;
//			this.a = a;
//			this.e = e;
//		}
//	}
//}