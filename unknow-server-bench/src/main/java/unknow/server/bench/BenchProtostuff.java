package unknow.server.bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.openjdk.jmh.annotations.Benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.protostuff.CodedInput;
import io.protostuff.JsonIOUtil;
import io.protostuff.JsonXIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffOutput;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import unknow.server.http.test.xml.Complex;

public class BenchProtostuff {
	private static final Complex TEST;

	private static final Schema<Complex> SCHEMA = RuntimeSchema.createFrom(Complex.class);

	private static final ObjectMapper m = new ObjectMapper();

	static {
		try (Reader r = new StringReader(BenchJaxb.XML)) {
			TEST = (Complex) BenchJaxb.MOXY.createUnmarshaller().unmarshal(r);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	public void jackson() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.writeValue(out, TEST);
		out.flush();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		m.readValue(in, Complex.class);
	}

	@Benchmark
	public void protostuff() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		SCHEMA.writeTo(new ProtostuffOutput(buffer, out), TEST);
		LinkedBuffer.writeTo(out, buffer);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Complex t = SCHEMA.newMessage();
		SCHEMA.mergeFrom(new CodedInput(in, true), t);
	}

	@Benchmark
	public void protostuffJson() throws IOException {

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		byte[] byteArray = JsonXIOUtil.toByteArray(TEST, SCHEMA, false, buffer);

		Complex t = SCHEMA.newMessage();
		JsonIOUtil.mergeFrom(byteArray, t, SCHEMA, false);
	}

}
