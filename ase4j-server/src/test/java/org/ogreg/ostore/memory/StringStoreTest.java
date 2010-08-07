package org.ogreg.ostore.memory;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import java.util.HashMap;


/**
 * String index tests.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "correctness")
public class StringStoreTest {

    /**
     * Tests some corner cases (since most of StringIndex is covered by other tests).
     */
    @Test public void testCoverage01() throws Exception {
        StringStore test = new StringStore();
        test.init(null, null, new HashMap<String, String>());

        // For coverage
        test.add(1, "aaa");
        test.save("ccc");
        test.saveOrUpdate("ccc");

        assertEquals(test.uniqueResult("", "aaa"), Long.valueOf(1));
        assertEquals(test.uniqueResult("", "bbb"), null);
        assertEquals(test.uniqueResult("", "ccc"), Long.valueOf(1));
        assertEquals(test.get(10), null);
        assertEquals(test.get(1), "ccc");
    }
}
