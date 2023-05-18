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
	 * @return generic param
	 */
	List<ParamModel> parameters();

	/**
	 * @param i index of the parameter to get
	 * @return get the i'th parameter
	 */
	default ParamModel parameter(int i) {
		return parameters().get(i);
	}

	/**
	 * @return method signature name(parameters type)return type
	 */
	default String signature() {
		StringBuilder sb = new StringBuilder(name()).append('(');
		if (!parameters().isEmpty()) {
			for (ParamModel t : parameters())
				sb.append(t.type().internalName()).append(',');
			sb.setLength(sb.length() - 1);
		}
		sb.append(')').append(type().internalName());
		return sb.toString();
	}
}
