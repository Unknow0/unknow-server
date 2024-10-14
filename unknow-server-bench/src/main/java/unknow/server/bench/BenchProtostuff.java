package unknow.server.bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

import io.protostuff.CodedInput;
import io.protostuff.JsonIOUtil;
import io.protostuff.JsonXIOUtil;
import io.protostuff.JsonXIOUtil2;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffOutput;
import unknow.server.bench.protobuf.TestMsg.Builder;
import unknow.server.bench.protostuff.TestMsg;

public class BenchProtostuff {
	private static final ObjectMapper m = new ObjectMapper();
	private static final ObjectMapper p = new ProtobufMapper();
	private static final ProtobufSchema schema;
	static {
		try {
			schema = ProtobufSchemaLoader.std.load(BenchProtostuff.class.getResourceAsStream("/test.proto"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final TestMsg TSTUFF;
	private static final unknow.server.bench.protobuf.TestMsg TBUF;
	private static final Pojo POJO;
	static {
		Builder n = unknow.server.bench.protobuf.TestMsg.newBuilder();
		TestMsg t = new TestMsg();
		Pojo p = new Pojo();
		for (int i = 0; i < 50; i++) {
			p = new Pojo().setLoop(Arrays.asList(p.setTest("test" + i).setV(i)));
			t = new TestMsg().setLoopList(Arrays.asList(t.setTest("test" + i).setV(i)));
			n = unknow.server.bench.protobuf.TestMsg.newBuilder().addLoop(n.setTest("test" + i).setV(i));
		}
		TSTUFF = t;
		TBUF = n.build();
		POJO = p;
	}

	@Benchmark
	public void jackson() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.writeValue(out, POJO);
		out.flush();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		m.readValue(in, Pojo.class);
	}

	@Benchmark
	public void jacksonProto() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		p.writer(schema).writeValue(out, POJO);
		out.flush();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		p.reader(schema).readValue(in, Pojo.class);
	}

	@Benchmark
	public void protostuff() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		TSTUFF.cachedSchema().writeTo(new ProtostuffOutput(buffer, out), TSTUFF);
		LinkedBuffer.writeTo(out, buffer);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TestMsg t = new TestMsg();
		TSTUFF.cachedSchema().mergeFrom(new CodedInput(in, true), t);
	}

	@Benchmark
	public void protostuffJson() throws IOException {

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		byte[] byteArray = JsonIOUtil.toByteArray(TSTUFF, TSTUFF.cachedSchema(), false, buffer);

		TestMsg t = new TestMsg();
		JsonIOUtil.mergeFrom(byteArray, t, TSTUFF.cachedSchema(), false);
	}

	@Benchmark
	public void protostuffXJson() throws IOException {

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		byte[] byteArray = JsonXIOUtil.toByteArray(TSTUFF, TSTUFF.cachedSchema(), false, buffer);

		TestMsg t = new TestMsg();
		JsonXIOUtil2.mergeFrom(byteArray, t, TSTUFF.cachedSchema(), false);
	}

	@Benchmark
	public void protobuf() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TBUF.writeTo(out);
		TBUF.getParserForType().parseFrom(out.toByteArray());
	}

	public static class Pojo {
		String test;
		Integer v;
		List<Pojo> loop;

		public Pojo() {
		}

		public String getTest() {
			return test;
		}

		public Pojo setTest(String test) {
			this.test = test;
			return this;
		}

		public Integer getV() {
			return v;
		}

		public Pojo setV(Integer v) {
			this.v = v;
			return this;
		}

		public List<Pojo> getLoop() {
			return loop;
		}

		public Pojo setLoop(List<Pojo> loop) {
			this.loop = loop;
			return this;
		}

	}
}
