package org.ogreg.ase4j.file;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * Association storage tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class AssociationBlockTest {

	/**
	 * Tests a simple association merge.
	 */
	public void testMerge01() {
		AssociationBlock.baseCapacity = 4;

		AssociationBlock row = new AssociationBlock(1);

		assertEquals(1, row.from);
		assertEquals(0, row.size());
		assertEquals("[0, 0, 0, 0]", Arrays.toString(row.tos));
		assertEquals("[0.0, 0.0, 0.0, 0.0]", Arrays.toString(row.values));

		row.merge(2, 100, Operation.OVERWRITE);
		row.merge(1, 101, Operation.OVERWRITE);
		row.merge(3, 100, Operation.OVERWRITE);

		assertEquals(1, row.from);
		assertEquals(3, row.size());
		assertEquals("[1, 2, 3, 0]", Arrays.toString(row.tos));
		assertEquals("[101.0, 100.0, 100.0, 0.0]", Arrays.toString(row.values));
	}

	/**
	 * Tests an association update.
	 */
	public void testMerge02() {
		AssociationBlock.baseCapacity = 2;

		AssociationBlock row = new AssociationBlock(1);

		assertEquals("[0, 0]", Arrays.toString(row.tos));
		assertEquals("[0.0, 0.0]", Arrays.toString(row.values));

		row.merge(1, 100, Operation.OVERWRITE);
		row.merge(1, 100, Operation.OVERWRITE); // No change for coverage
		row.merge(1, 200, Operation.OVERWRITE);

		assertTrue(row.isChanged());
		assertEquals(1, row.size());
		assertEquals("[1, 0]", Arrays.toString(row.tos));
		assertEquals("[200.0, 0.0]", Arrays.toString(row.values));

		// Some gets for coverage
		assertEquals(200.0F, row.get(1));
		assertEquals(0.0F, row.get(0));
	}

	/**
	 * Tests a more complicated association merge.
	 */
	public void testMerge03() {
		AssociationBlock.baseCapacity = 2;

		AssociationBlock row1 = new AssociationBlock(1);
		AssociationBlock row2 = new AssociationBlock(1);

		row1.merge(1, 100, Operation.OVERWRITE);
		row2.merge(1, 200, Operation.OVERWRITE);
		row1.merge(row2, Operation.OVERWRITE);

		assertEquals(1, row1.size());
		assertEquals("[1, 0]", Arrays.toString(row1.tos));
		assertEquals("[200.0, 0.0]", Arrays.toString(row1.values));
	}

	/**
	 * Tests that rows with different subjects can not be merged.
	 */
	public void testMerge04() {
		AssociationBlock.baseCapacity = 2;

		AssociationBlock row1 = new AssociationBlock(1);
		AssociationBlock row2 = new AssociationBlock(2);

		try {
			row1.merge(row2, Operation.OVERWRITE);

			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	/**
	 * Tests a row growing.
	 */
	public void testGrow01() {
		AssociationBlock.baseCapacity = 2;

		AssociationBlock row = new AssociationBlock(1);

		assertEquals(1, row.from);
		assertEquals(0, row.size());
		assertEquals("[0, 0]", Arrays.toString(row.tos));
		assertEquals("[0.0, 0.0]", Arrays.toString(row.values));

		row.merge(2, 100, Operation.OVERWRITE);
		row.merge(1, 101, Operation.OVERWRITE);

		assertFalse(row.isGrown());

		row.merge(3, 100, Operation.OVERWRITE);

		assertEquals(1, row.from);
		assertEquals(3, row.size());
		assertEquals(4, row.capacity());
		assertTrue(row.isGrown());
		assertEquals("[1, 2, 3, 0]", Arrays.toString(row.tos));
		assertEquals("[101.0, 100.0, 100.0, 0.0]", Arrays.toString(row.values));
	}

	/**
	 * Tests the row persistence.
	 */
	public void testPersist01() {

		try {
			AssociationBlock.baseCapacity = 2;

			AssociationBlock row = new AssociationBlock(1);

			row.merge(2, 100, Operation.OVERWRITE);
			row.merge(1, 101, Operation.OVERWRITE);

			File tmpFile = FileTestSupport.createTempFile("assoc.dat");

			RandomAccessFile tmp = new RandomAccessFile(tmpFile, "rw");
			MappedByteBuffer buf = tmp.getChannel().map(MapMode.READ_WRITE, 0,
					12 + (row.capacity() * 8));

			CachedBlockStore.Serializer.serialize(row, buf);

			FileTestSupport.assertBinaryEqual("assocs/assoc01.dat", tmpFile.getAbsolutePath());

			row = CachedBlockStore.Serializer.deserialize(buf);

			assertEquals(2, row.size());
			assertEquals(2, row.capacity());
			assertEquals("[1, 2]", Arrays.toString(row.tos));
			assertEquals("[101.0, 100.0]", Arrays.toString(row.values));
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
}
