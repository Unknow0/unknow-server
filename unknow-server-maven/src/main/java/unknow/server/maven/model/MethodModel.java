/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithMod;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public interface MethodModel extends WithAnnotation, WithMod, WithName, WithType {

	/**
	 * @return class owner
	 */
	ClassModel parent();

	/**
	 * @return method parameters
	 */
	List<ParamModel> parameters();

	/**
	 * @return method signature name(parameters type)return type
	 */
	default String signature() {
		StringBuilder sb = new StringBuilder(name()).append('(');
		for (ParamModel t : parameters())
			sb.append(t.type().internalName()).append(',');
		sb.setCharAt(sb.length() - 1, ')');
		sb.append(type().internalName());
		return sb.toString();
	}
}
