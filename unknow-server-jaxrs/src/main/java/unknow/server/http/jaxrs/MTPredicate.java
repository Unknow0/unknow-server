/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import jakarta.ws.rs.core.MediaType;

/**
 * @author unknow
 */
public class MTPredicate implements Predicate<MediaType> {
	private final Collection<MediaType> mts;

	public MTPredicate(MediaType... mts) {
		this.mts = Arrays.asList(mts);
	}

	@Override
	public boolean test(MediaType t) {
		for (MediaType m : mts) {
			if (t.isCompatible(m))
				return true;
		}
		return false;
	}
}
