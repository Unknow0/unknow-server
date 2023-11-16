/**
 * 
 */
package unknow.server.http.jaxrs.header;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import unknow.server.http.jaxrs.builder.LinkBuilderImpl;

/**
 * @author unknow
 */
public class LinkDelegate implements HeaderDelegate<Link> {

	public static final LinkDelegate INSTANCE = new LinkDelegate();

	private static final String REL = "rel";
	private static final String TYPE = "type";
	private static final String TITLE = "title";

	private static final Set<String> KNOWN_PARAMETERS = new HashSet<>(Arrays.asList(REL, TYPE, TITLE));

	@Override
	public Link fromString(String value) {

		if (value == null) {
			throw new IllegalArgumentException("Link value can not be null");
		}
		value = value.trim();
		int closeIndex = value.indexOf('>');
		if (!value.startsWith("<") || closeIndex < 2) {
			throw new IllegalArgumentException("Link URI is missing");
		}
		Link.Builder builder = new LinkBuilderImpl();
		builder.uri(value.substring(1, closeIndex).trim());
		if (closeIndex < value.length() - 1) {

			String[] tokens = value.substring(closeIndex + 1).split(";");
			for (String token : tokens) {
				String theToken = token.trim();
				if (theToken.isEmpty()) {
					continue;
				}
				String paramName = null;
				String paramValue = null;
				int i = token.indexOf('=');
				if (i != -1) {
					paramName = theToken.substring(0, i).trim();
					paramValue = i == theToken.length() - 1 ? "" : theToken.substring(i + 1).trim();
				}
				if (REL.equals(paramName)) {
					String[] rels = removeQuotesIfNeeded(paramValue).split(",");
					for (String rel : rels) {
						builder.rel(rel.trim());
					}
				} else if (TYPE.equals(paramName)) {
					builder.type(removeQuotesIfNeeded(paramValue));
				} else if (TITLE.equals(paramName)) {
					builder.title(removeQuotesIfNeeded(paramValue));
				} else {
					builder.param(paramName, paramValue);
				}
			}
		}
		return builder.build();

	}

	private static String removeQuotesIfNeeded(String value) {
		if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	@Override
	public String toString(Link link) {
		StringBuilder sb = new StringBuilder();

		sb.append('<');
		sb.append(link.getUri());
		sb.append('>');

		String rels = link.getRel();
		if (!rels.isEmpty()) {
			sb.append(';').append(REL).append('=');
			writeListParamValues(sb, rels);
		}
		if (link.getTitle() != null) {
			sb.append(';').append(TITLE).append("=\"").append(link.getTitle()).append('"');
		}
		if (link.getType() != null) {
			sb.append(';').append(TYPE).append('=').append(link.getType());
		}
		for (Map.Entry<String, String> entry : link.getParams().entrySet()) {
			if (KNOWN_PARAMETERS.contains(entry.getKey())) {
				continue;
			}
			sb.append(';').append(entry.getKey()).append('=');
			writeListParamValues(sb, entry.getValue());
		}

		return sb.toString();
	}

	private void writeListParamValues(StringBuilder sb, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		boolean commaAvailable = value.contains(",");
		if (commaAvailable) {
			sb.append('"');
		}
		sb.append(value);
		if (commaAvailable) {
			sb.append('"');
		}
	}
}
