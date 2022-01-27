/**
 * 
 */
package unknow.server.maven.jaxws.model;

import com.github.javaparser.ast.expr.Expression;

import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class XmlTypeWrapper implements XmlType {
	public XmlType delegate;

	public XmlTypeWrapper() {
	}

	public void setDelegate(XmlType delegate) {
		this.delegate = delegate;
	}

	@Override
	public String clazz() {
		return delegate.clazz();
	}

	@Override
	public String binaryName() {
		return delegate.binaryName();
	}

	@Override
	public boolean isPrimitive() {
		return delegate.isPrimitive();
	}

	@Override
	public Expression convert(TypeCache types, Expression v) {
		return delegate.convert(types, v);
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return delegate.toString(types, v);
	}

	@Override
	public boolean isSimple() {
		return delegate.isSimple();
	}

	@Override
	public SchemaData schema() {
		return delegate.schema();
	}
}
