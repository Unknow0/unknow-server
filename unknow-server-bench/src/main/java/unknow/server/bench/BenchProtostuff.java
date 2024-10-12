package unknow.server.bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.protostuff.CodedInput;
import io.protostuff.JsonIOUtil;
import io.protostuff.JsonXIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffOutput;
import unknow.server.bench.protobuf.TestMsg.Builder;
import unknow.server.bench.protostuff.TestMsg;

public class BenchProtostuff {
	private static final ObjectMapper m = new ObjectMapper();

	private static final TestMsg TSTUFF;
	private static final unknow.server.bench.protobuf.TestMsg TBUF;
	static {
		Builder n = unknow.server.bench.protobuf.TestMsg.newBuilder();
		TestMsg t = new TestMsg();
		for (int i = 0; i < 50; i++) {
			t = new TestMsg().setLoopList(Arrays.asList(t.setTest("test" + i).setV(i)));
			n = unknow.server.bench.protobuf.TestMsg.newBuilder().addLoop(n.setTest("test" + i).setV(i));
		}
		TSTUFF = t;
		TBUF = n.build();
	}

	@Benchmark
	public void jackson() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.writeValue(out, TSTUFF);
		out.flush();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		m.readValue(in, TestMsg.class);
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
		JsonIOUtil.mergeFrom(byteArray, t, TSTUFF.cachedSchema(), false);
	}

	@Benchmark
	public void protobuf() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TBUF.writeTo(out);
		TBUF.getParserForType().parseFrom(out.toByteArray());
	}
}
