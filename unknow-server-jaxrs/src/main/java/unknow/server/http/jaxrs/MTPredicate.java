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

	boolean isCompatible(MediaType t);

	public static MTPredicate ANY = new MTPredicate() {

		@Override
		public MediaType getMatching(MediaType t) {
			return t;
		}

		@Override
		public boolean isCompatible(MediaType t) {
			return true;
		}
	};
	public static MTPredicate NONE = new MTPredicate() {

		@Override
		public MediaType getMatching(MediaType t) {
			return null;
		}

		@Override
		public boolean isCompatible(MediaType t) {
			return false;
		}
	};

	public static class Single implements MTPredicate {
		private final MediaType mt;

		public Single(MediaType mt) {
			this.mt = mt;
		}

		@Override
		public MediaType getMatching(MediaType t) {
			String charset = t.getParameters().get(MediaType.CHARSET_PARAMETER);
			if (t.isWildcardType()) {
				if (t.isWildcardSubtype() || t.getSubtype().equals(mt.getSubtype())) {
					if (charset == null)
						return mt;
					return new MediaType(mt.getType(), mt.getSubtype(), charset);
				}
			} else if (t.getType().equals(mt.getType())) {
				if (t.isWildcardSubtype()) {
					if (charset == null)
						return mt;
					return new MediaType(mt.getType(), mt.getSubtype(), charset);
				}
				if (t.getSubtype().equals(mt.getSubtype()))
					return t;
			}
			return null;
		}

		@Override
		public boolean isCompatible(MediaType t) {
			return t != null && t.isCompatible(mt);
		}
	}

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

		@Override
		public boolean isCompatible(MediaType t) {
			if (t == null)
				return false;
			for (int i = 0; i < mts.length; i++) {
				if (t.isCompatible(mts[i]))
					return true;
			}
			return false;
		}
	}
}
