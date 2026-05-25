package unknow.server.maven;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import unknow.maven.codegen.TypeFactory;

public interface SourceBuilder<T> {

	void process(ClassOrInterfaceDeclaration cl, TypeFactory types, T ctx);

	public abstract class AbstractSourceBuilder<T> implements SourceBuilder<T> {
		protected ClassOrInterfaceDeclaration cl;
		protected TypeFactory types;
		protected T ctx;

		@Override
		public final void process(ClassOrInterfaceDeclaration cl, TypeFactory types, T ctx) {
			this.cl = cl;
			this.types = types;
			this.ctx = ctx;
			build();
		}

		protected abstract void build();
	}
}
