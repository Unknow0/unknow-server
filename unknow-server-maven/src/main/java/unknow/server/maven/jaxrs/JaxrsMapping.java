/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.util.List;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;

public class JaxrsMapping {
	public final String v;
	public final ClassModel clazz;
	public final MethodModel m;
	public final String httpMethod;
	public final String path;
	public final List<JaxrsParam<?>> params;
	public final String[] consume;
	public final String[] produce;

	public JaxrsMapping(String v, ClassModel clazz, MethodModel m, String httpMethod, List<JaxrsParam<?>> params, String path, String[] consume, String[] produce) {
		this.v = v;
		this.clazz = clazz;
		this.m = m;
		this.httpMethod = httpMethod;
		this.params = params;
		this.path = path;
		this.consume = consume;
		this.produce = produce;
	}
}