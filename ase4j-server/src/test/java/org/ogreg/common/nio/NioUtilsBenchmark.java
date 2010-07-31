package org.ogreg.common.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * NIO benchmarks.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class NioUtilsBenchmark {

	/**
	 * Measures NIO move performance.
	 */
	public void testMove01() {

		try {
			File file = FileTestSupport.createTempFile("move01");
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			FileChannel ch = raf.getChannel();

			ByteBuffer test = ByteBuffer.allocate(1024 * 1024);
			test.putInt(0, 1);
			test.putInt(4, 2);
			test.putInt(test.capacity() - 4, 3);

			for (int i = 0; i < 100; i++) {
				test.position(0);
				ch.write(test, i * test.capacity());
			}

			int ITERATIONS = 100;

			for (int i = 0; i < ITERATIONS; i++) {
				test.position(0);
				ch.write(test, i * test.capacity());
			}

			long before = System.currentTimeMillis();
			NioUtils.move(raf.getChannel(), 4, 8);

			long time = System.currentTimeMillis() - before;

			System.err.printf("%.2f Mb/s\n", (double) file.length() / 1024 / 1.024 / time);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Measures NIO readInt (buffer cache) performance.
	 */
	public void testReadInt01() {

		try {
			File file = FileTestSupport.createTempFile("readInt01");
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			FileChannel ch = raf.getChannel();

			ByteBuffer test = ByteBuffer.allocate(4);
			test.putInt(0, 1);

			for (int i = 0; i < 100; i++) {
				test.position(0);
				ch.write(test);
			}

			int ITERATIONS = 100000;
			int[] dummy = new int[1];
			Random r = new Random(0);

			long before = System.currentTimeMillis();

			for (int i = 0; i < ITERATIONS; i++) {
				dummy[0] = NioUtils.readInt(ch, r.nextInt(100));
			}

			long time = System.currentTimeMillis() - before;

			System.err.printf("%.2f getInt/s\n", ITERATIONS * 1000.0 / time);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
