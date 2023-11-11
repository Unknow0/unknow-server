/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithMod;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithParent;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public interface MethodModel extends WithAnnotation, WithMod, WithName, WithType, WithParent<ClassModel> {

	/**
	 * @return generic param
	 */
	List<ParamModel<MethodModel>> parameters();

	/**
	 * @return annotation default value
	 */
	AnnotationValue defaultValue();

	/**
	 * @param i index of the parameter to get
	 * @return get the i'th parameter
	 */
	default ParamModel<MethodModel> parameter(int i) {
		return parameters().get(i);
	}

	/**
	 * @return method signature name(parameters type)return type
	 */
	default String signature() {
		StringBuilder sb = new StringBuilder(name()).append('(');
		if (!parameters().isEmpty()) {
			for (ParamModel<MethodModel> t : parameters())
				sb.append(t.type().name()).append(',');
			sb.setLength(sb.length() - 1);
		}
		sb.append(')').append(type().name());
		return sb.toString();
	}
}
