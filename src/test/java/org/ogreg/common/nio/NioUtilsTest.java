package org.ogreg.common.nio;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * NIO Utils tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class NioUtilsTest {

	/**
	 * Tests file growing.
	 */
	public void testMove01() throws IOException {
		NioUtils.BLOCK_SIZE = 3;

		byte[] a;

		a = move(1, 1);
		assertEquals(new byte[] { 0, 1, 2, 3, 0, 0 }, a);

		a = move(0, 1);
		assertEquals(new byte[] { 0, 0, 1, 2, 3, 0 }, a);

		a = move(1, 2);
		assertEquals(new byte[] { 0, 1, 1, 2, 3, 0 }, a);

		a = move(3, 5);
		assertEquals(new byte[] { 0, 1, 2, 3, 0, 3 }, a);
	}

	/**
	 * Tests file shrinking.
	 */
	public void testMove02() throws IOException {
		NioUtils.BLOCK_SIZE = 2;

		byte[] a;

		a = move(1, 0);
		assertEquals(new byte[] { 1, 2, 3, 0, 0, 0 }, a);

		a = move(2, 0);
		assertEquals(new byte[] { 2, 3, 0, 0, 0, 0 }, a);
	}

	/**
	 * Tests pattern fill.
	 */
	public void testFill01() throws IOException {
		NioUtils.BLOCK_SIZE = 2;

		byte[] a;

		a = fill(1, 2, (byte) 0);
		assertEquals(new byte[] { 0, 0, 2, 3, 0, 0 }, a);

		a = fill(0, 6, (byte) 0xFF);
		assertEquals(new byte[] { -1, -1, -1, -1, 0, 0 }, a);

		a = fill(3, 2, (byte) 0);
		assertEquals(new byte[] { 0, 1, 2, 3, 0, 0, }, a);
	}

	/**
	 * Tests Java2NIO serialization and deserialization.
	 */
	public void testSerialize01() throws IOException {
		RandomAccessFile raf = new RandomAccessFile(FileTestSupport.createTempFile("serialize01"),
				"rw");
		FileChannel channel = raf.getChannel();

		try {
			Object wrote = String.class;
			NioUtils.serializeTo(channel, wrote);

			channel.position(0);

			Object read = NioUtils.deserializeFrom(channel, Class.class);

			assertEquals(read, wrote);
		} finally {
			NioUtils.closeQuietly(channel);
		}
	}

	/**
	 * Tests some dummy cases for coverage.
	 */
	public void testCoverage01() throws IOException {
		// Unmapping a null buffer
		NioUtils.unmap(null);
	}

	protected byte[] move(int from, int to) {
		FileChannel fc = null;

		try {
			File test = FileTestSupport.createTempFile("move01");
			fc = new RandomAccessFile(test, "rw").getChannel();

			ByteBuffer tb = testBuf(4);
			fc.write(tb, 0);

			NioUtils.move(fc, from, to);

			ByteBuffer dst = ByteBuffer.allocate(6);
			fc.read(dst, 0);

			return dst.array();
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			close(fc);
		}
	}

	protected byte[] fill(int from, int to, byte pattern) {
		FileChannel fc = null;

		try {
			File test = FileTestSupport.createTempFile("fill01");
			fc = new RandomAccessFile(test, "rw").getChannel();

			ByteBuffer tb = testBuf(4);
			fc.write(tb, 0);

			NioUtils.fill(fc, from, to, pattern);

			ByteBuffer dst = ByteBuffer.allocate(6);
			fc.read(dst, 0);

			return dst.array();
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			close(fc);
		}
	}

	ByteBuffer testBuf(int len) {
		ByteBuffer tb = ByteBuffer.allocate(len);

		for (int i = 0; i < len; i++) {
			tb.put(i, (byte) i);
		}

		return tb;
	}

	void close(FileChannel fc) {

		try {

			if (fc != null) {
				fc.close();
			}
		} catch (IOException e) {
		}
	}
}
