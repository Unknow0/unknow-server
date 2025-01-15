/**
 * 
 */
package unknow.server.http.jaxrs;

import jakarta.ws.rs.core.MediaType;

/**
 * @author unknow
 */
public interface MTPredicate {
	MediaType getMatching(MediaType t);

	public static MTPredicate ANY = t -> t;

	public static class OneOf implements MTPredicate {
		private final MediaType[] mts;

		public OneOf(MediaType... mts) {
			this.mts = mts;
		}

		@Override
		public MediaType getMatching(MediaType t) {
			String charset = t.getParameters().get(MediaType.CHARSET_PARAMETER);
			for (int i = 0; i < mts.length; i++) {
				MediaType m = mts[i];
				if (t.isWildcardType()) {
					if (t.isWildcardSubtype() || t.getSubtype().equals(m.getSubtype())) {
						if (charset == null)
							return m;
						return new MediaType(m.getType(), m.getSubtype(), charset);
					}
				} else if (t.getType().equals(m.getType())) {
					if (t.isWildcardSubtype()) {
						if (charset == null)
							return m;
						return new MediaType(m.getType(), m.getSubtype(), charset);
					}
					if (t.getSubtype().equals(m.getSubtype()))
						return t;
				}
			}
			return null;
		}
	}
}
