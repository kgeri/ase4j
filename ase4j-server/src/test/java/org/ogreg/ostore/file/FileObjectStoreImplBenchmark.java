package org.ogreg.ostore.file;

import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;

import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;
import org.ogreg.test.TestUtils.Measurement;
import org.ogreg.test.TestUtils.Result;

import org.testng.annotations.Test;

import java.io.File;
import java.io.Flushable;

import java.util.Date;


/**
 * Object store benchmarks.
 *
 * @author  Gergely Kiss
 */
@SuppressWarnings("unchecked")
@Test(groups = "performance")
public class FileObjectStoreImplBenchmark {
    private ObjectStore<TestData> store;
    private File dir;

    /**
     * Tests some random inserts in the object store.
     */
    public void testInsert01() throws Exception {
        ObjectStoreManager config = new ObjectStoreManager();
        config.add("configuration/test-ostore-performance.xml");

        dir = FileTestSupport.createTempDir("perf");
        store = config.getStore("test", dir);

        Result r = TestUtils.measure(100000, new Measurement() {
                    @Override public void run(int iteration) throws Exception {
                        TestData data = new TestData();
                        data.url = "http://www." + iteration;
                        data.created = new Date();
                        data.contents = TestUtils.randomString(50, 200);

                        store.save(data);
                    }
                });

        ((Flushable) store).flush();

        System.err.printf("%.2f appends per sec (%.2f Mb/s)\n", r.stepsPerSec,
            (FileTestSupport.length(dir) / 1024.0 / 1.024 / r.timeMs));
    }

    /**
     * Tests some random reads from the object store.
     *
     * <p>This test measures how fast the store can synthesize objects (used a lot when filtering).</p>
     */
    @Test(dependsOnMethods = "testInsert01")
    public void testGet01() throws Exception {

        Result r = TestUtils.measure(100000, new Measurement() {
                    @Override public void run(int iteration) throws Exception {
                        store.get(iteration);
                    }
                });

        System.err.printf("%.2f gets per sec (%.2f Mb/s)\n", r.stepsPerSec,
            (FileTestSupport.length(dir) / 1024.0 / 1.024 / r.timeMs));
    }

    /**
     * Tests some unique key finds.
     *
     * <p>This test measures how fast the store can find object ids by their unique keys (currently using a Trie with a
     * URL dictionary).</p>
     */
    @Test(dependsOnMethods = "testInsert01")
    public void testUnique01() throws Exception {

        Result r = TestUtils.measure(100000, new Measurement() {
                    @Override public void run(int iteration) throws Exception {
                        store.uniqueResult("url", "http://www." + iteration);
                    }
                });

        System.err.printf("%.2f unique finds per sec (%.2f Mb/s)\n", r.stepsPerSec,
            (FileTestSupport.length(dir) / 1024.0 / 1.024 / r.timeMs));
    }

    static class TestData {
        String url;
        Date created;
        String contents;
    }
}
