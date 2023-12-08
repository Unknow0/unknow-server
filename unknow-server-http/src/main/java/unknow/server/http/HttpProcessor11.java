package unknow.server.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.UnavailableException;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;
import unknow.server.util.io.BuffersUtils.IndexOfBloc;

public class HttpProcessor11 extends HttpProcessor {
	private static final Logger logger = LoggerFactory.getLogger(HttpProcessor11.class);

	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte[] PARAM_SEP = { '&', '=' };
	private static final byte[] SPACE_SLASH = { ' ', '/' };
	private static final byte SPACE = ' ';
	private static final byte QUESTION = '?';
	private static final byte COLON = ':';
	private static final byte SEMICOLON = ';';
	private static final byte SLASH = '/';
	private static final byte AMPERSAMP = '&';
	private static final byte EQUAL = '=';

	private static final int MAX_METHOD_SIZE = 10; // max size for method
	private static final int MAX_PATH_SIZE = 2000;
	private static final int MAX_VERSION_SIZE = 12;
	private static final int MAX_HEADER_SIZE = 512;

	private static final int MAX_START_SIZE = 8192;

	private final IndexOfBloc end;

	private final StringBuilder sb;
	private final Decode decode;

	public HttpProcessor11(ServletContextImpl ctx, int keepAliveIdle) {
		super(ctx, keepAliveIdle);

		end = new IndexOfBloc(new byte[] { '\r', '\n', '\r', '\n' });
		sb = new StringBuilder();
		decode = new Decode(sb);
	}

	@Override
	protected boolean canProcess(HttpConnection co) throws InterruptedException {
		end.reset();
		co.pendingRead.walk(end, 0, MAX_START_SIZE);
		return end.index() > 0;
	}

	@Override
	protected boolean fillRequest() throws InterruptedException, IOException {
		Buffers b = readBuffer();
		int i = BuffersUtils.indexOf(b, SPACE_SLASH, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			res.sendError(HttpError.BAD_REQUEST.code);
			return false;
		}
		BuffersUtils.toString(sb, b, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = BuffersUtils.indexOf(b, SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			res.sendError(HttpError.URI_TOO_LONG.code);
			return false;
		}
		int q = BuffersUtils.indexOf(b, QUESTION, last, i - last);
		if (q < 0)
			q = i;

		b.walk(decode, last, q - last);
		if (!decode.done())
			return false;
		req.setRequestUri(sb.toString());
		sb.setLength(0);

		int s;
		while ((s = BuffersUtils.indexOf(b, SLASH, last + 1, q - last - 1)) > 0) {
			int c = BuffersUtils.indexOf(b, SEMICOLON, last + 1, s - last - 1);
			b.walk(decode, last + 1, (c < 0 ? s : c) - last - 1);
			if (!decode.done())
				return false;
			req.addPath(sb.toString());
			sb.setLength(0);
			last = s;
		}
		if (s == -2 && last + 1 < q) {
			int c = BuffersUtils.indexOf(b, SEMICOLON, last + 1, q - last - 1);
			BuffersUtils.toString(sb, b, last + 1, c < 0 ? q - last - 1 : c);
			req.addPath(sb.toString());
			sb.setLength(0);
		}

		if (q < i) {
			BuffersUtils.toString(sb, b, q + 1, i - q - 1);
			req.setQuery(sb.toString());
			sb.setLength(0);
		} else
			req.setQuery("");

		Map<String, List<String>> map = new HashMap<>();
		parseParam(map, b, q + 1, i);
		req.setQueryParam(map);
		last = i + 1;

		i = BuffersUtils.indexOf(b, CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			res.sendError(HttpError.BAD_REQUEST.code);
			return false;
		}
		BuffersUtils.toString(sb, b, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		map = new HashMap<>();
		while ((i = BuffersUtils.indexOf(b, CRLF, last, MAX_HEADER_SIZE)) > last) {
			int c = BuffersUtils.indexOf(b, COLON, last, i - last);
			if (c < 0) {
				res.sendError(HttpError.BAD_REQUEST.code);
				return false;
			}

			BuffersUtils.toString(sb, b, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			BuffersUtils.toString(sb, b, c + 1, i - c - 1);
			String v = sb.toString().trim();
			sb.setLength(0);

			List<String> list = map.get(k);
			if (list == null)
				map.put(k, list = new ArrayList<>(1));
			list.add(v);

			last = i + 2;
		}
		req.setHeaders(map);
		b.skip(last + 2);
		return true;
	}

	private boolean parseParam(Map<String, List<String>> map, Buffers data, int o, int e) throws InterruptedException {
		while (o < e) {
			int i = BuffersUtils.indexOfOne(data, PARAM_SEP, o, e - o);
			if (i < 0)
				i = e;
			data.walk(decode, o, i - o);
			if (!decode.done())
				return false;
			String key = sb.toString();
			sb.setLength(0);

			o = i + 1;
			if (i < e && data.get(i) == EQUAL) {
				i = BuffersUtils.indexOf(data, AMPERSAMP, o, e - o);
				if (i < 0)
					i = e;
				data.walk(decode, o, i - o);
				if (!decode.done())
					return false;
				o = i + 1;
			}
			map.computeIfAbsent(key, k -> new ArrayList<>(1)).add(sb.toString());
			sb.setLength(0);
		}
		return true;
	}

	@Override
	protected final void doRun(ServletRequestImpl req, ServletResponseImpl res) throws IOException {
		FilterChain s = servlets.find(req);
		try {
			s.doFilter(req, res);
		} catch (UnavailableException e) {
			// TODO add page with retry-after
			res.sendError(503, e, null);
		} catch (Exception e) {
			logger.error("failed to service '{}'", s, e);
			if (!res.isCommitted())
				res.sendError(500);
		}
	}

}
