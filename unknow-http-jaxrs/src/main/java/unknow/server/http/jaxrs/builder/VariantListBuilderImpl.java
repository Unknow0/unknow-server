/**
 * 
 */
package unknow.server.http.jaxrs.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.core.Variant.VariantListBuilder;

/**
 * @author unknow
 */
public class VariantListBuilderImpl extends VariantListBuilder {
	private List<String> encodings = new ArrayList<>();
	private List<Locale> languages = new ArrayList<>();
	private List<MediaType> mediaTypes = new ArrayList<>();
	private List<Variant> variants = new ArrayList<>();

	@Override
	public VariantListBuilder add() {
		addVariants();
		resetMeta();
		return this;
	}

	@Override
	public List<Variant> build() {
		addVariants();
		List<Variant> vs = new ArrayList<>(variants);
		reset();
		return vs;
	}

	@Override
	public VariantListBuilder encodings(String... encs) {
		Collections.addAll(encodings, encs);
		return this;
	}

	@Override
	public VariantListBuilder mediaTypes(MediaType... types) {
		Collections.addAll(mediaTypes, types);
		return this;
	}

	private void reset() {
		variants.clear();
		resetMeta();
	}

	private void resetMeta() {
		mediaTypes.clear();
		languages.clear();
		encodings.clear();
	}

	private void addVariants() {
		if (!mediaTypes.isEmpty()) {
			handleMediaTypes();
		} else if (!languages.isEmpty()) {
			handleLanguages(null);
		} else if (!encodings.isEmpty()) {
			for (String enc : encodings) {
				variants.add(new Variant(null, (Locale) null, enc));
			}
		}
	}

	private void handleMediaTypes() {
		for (MediaType type : mediaTypes) {
			if (!languages.isEmpty()) {
				handleLanguages(type);
			} else if (!encodings.isEmpty()) {
				for (String enc : encodings) {
					variants.add(new Variant(type, (Locale) null, enc));
				}
			} else {
				variants.add(new Variant(type, (Locale) null, null));
			}
		}
	}

	private void handleLanguages(MediaType type) {
		for (Locale lang : languages) {
			if (!encodings.isEmpty()) {
				for (String enc : encodings) {
					variants.add(new Variant(type, lang, enc));
				}
			} else {
				variants.add(new Variant(type, lang, null));
			}
		}
	}

	@Override
	public VariantListBuilder languages(Locale... ls) {
		Collections.addAll(languages, ls);
		return this;
	}
}
