/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.util.List;

import unknow.server.maven.jaxrs.JaxrsModel.PathPart;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;

public class JaxrsMapping {
	public final String var;
	public final ClassModel clazz;
	public final MethodModel m;
	public final List<JaxrsParam> params;
	public final List<PathPart> parts;
	public final String[] consume;
	public final String[] produce;

	public JaxrsMapping(String var, ClassModel clazz, MethodModel m, List<JaxrsParam> params, List<PathPart> parts, String[] consume, String[] produce) {
		this.var = var;
		this.clazz = clazz;
		this.m = m;
		this.params = params;
		this.parts = parts;
		this.consume = consume;
		this.produce = produce;
	}
}