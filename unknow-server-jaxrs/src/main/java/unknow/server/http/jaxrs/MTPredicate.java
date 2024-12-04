/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.function.Predicate;

import jakarta.ws.rs.core.MediaType;

/**
 * @author unknow
 */
public class MTPredicate implements Predicate<MediaType> {
	private final MediaType[] mts;

	public MTPredicate(MediaType... mts) {
		this.mts = mts;
	}

	@Override
	public boolean test(MediaType t) {
		for (int i = 0; i < mts.length; i++) {
			if (accept(mts[i], t))
				return true;
		}
		return false;
	}

	public static boolean accept(MediaType t, MediaType y) {
		if (!t.getType().equals(y.getType()) && t.isWildcardType())
			return false;
		return !t.getSubtype().equals(y.getSubtype()) && t.isWildcardSubtype();
	}
}
