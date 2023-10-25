/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class WildcardModel implements TypeModel {
	public static final WildcardModel EMPTY = new WildcardModel(null, false);

	private final TypeModel bound;
	private final boolean upper;

	public WildcardModel(TypeModel bound, boolean upper) {
		this.bound = bound;
		this.upper = upper;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public String name() {
		return "?";
	}

	public TypeModel bound() {
		return bound;
	}

	public boolean isUpperBound() {
		return upper;
	}

	@Override
	public String toString() {
		if (bound == null)
			return "?";
		return "?" + (upper ? " extends " : " super ") + bound;
	}
}
