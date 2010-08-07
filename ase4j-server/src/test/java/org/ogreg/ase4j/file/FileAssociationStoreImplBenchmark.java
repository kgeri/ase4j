package org.ogreg.ase4j.file;

import org.ogreg.ase4j.AssociationStoreException;
import org.ogreg.ase4j.criteria.Expression;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.Restrictions;

import org.ogreg.ostore.memory.StringStore;

import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;


/**
 * File based association store benchmarks.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "performance")
public class FileAssociationStoreImplBenchmark {
    FileAssociationStoreImpl<String, String> store = new FileAssociationStoreImpl<String, String>();
    private List<String> words;

    @BeforeTest public void before() {
        File file = FileTestSupport.createTempFile("store");
        StringStore ostore = new StringStore();
        ostore.init(null, null, new HashMap<String, String>());
        store.setFromStore(ostore);
        store.setToStore(ostore);
        store.setStorageFile(file);
        store.init();
    }

    @AfterTest public void after() throws IOException {
        store.close();
        words = null;
    }

    /**
     * Tests the performance of lots of random inserts.
     */
    public void testInsert01() {

        try {
            int ITERATIONS = 100000;

            words = TestUtils.randomWords(ITERATIONS, 31);

            long before = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                store.add(words.get(i), words.get(ITERATIONS - i - 1), 1.0F);
            }

            long time = System.currentTimeMillis() - before;

            System.err.println((ITERATIONS * 1000 / (time + 1)) + " inserts per sec");
            System.err.println(((double) store.getStorageFile().length() / 1024 / 1.024 / time) +
                " Mb/s");
        } catch (AssociationStoreException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests the performance of lots of inserts to the same subject.
     */
    public void testInsert02() {

        try {
            int ITERATIONS = 100000;

            words = TestUtils.randomWords(ITERATIONS, 31);

            long before = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                store.add("alma", words.get(i), 1.0F);
            }

            long time = System.currentTimeMillis() - before;

            System.err.println((ITERATIONS * 1000 / (time + 1)) + " same subject inserts per sec");
            System.err.println(((double) store.getStorageFile().length() / 1024 / 1.024 / time) +
                " Mb/s");
        } catch (AssociationStoreException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests the performance of lots of simple queries.
     */
    @Test(dependsOnMethods = "testInsert01")
    public void testQuery01() throws QueryExecutionException {
        int ITERATIONS = 100000;

        long before = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            store.query(new Query(Restrictions.phrase(words.get(i))).limit(100));
        }

        long time = System.currentTimeMillis() - before;

        System.err.println((ITERATIONS * 1000 / (time + 1)) + " simple queries per sec");
    }

    /**
     * Tests the performance of one enormous AND query.
     */
    @Test(dependsOnMethods = "testInsert01")
    public void testQuery02() throws QueryExecutionException {
        int ITERATIONS = 100000;

        Expression[] e = new Expression[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            e[i] = Restrictions.phrase(words.get(i));
        }

        long before = System.currentTimeMillis();

        store.query(new Query(Restrictions.and(e[0], e)).limit(100));

        long time = System.currentTimeMillis() - before;

        System.err.println((ITERATIONS * 1000 / (time + 1)) + " huge AND queries per sec");
    }

    /**
     * Tests the performance of one enormous OR query.
     */
    @Test(dependsOnMethods = "testInsert01")
    public void testQuery03() throws QueryExecutionException {
        int ITERATIONS = 1000;

        Expression[] e = new Expression[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            e[i] = Restrictions.phrase(words.get(i));
        }

        long before = System.currentTimeMillis();

        store.query(new Query(Restrictions.or(e[0], e)).limit(100));

        long time = System.currentTimeMillis() - before;

        System.err.println((ITERATIONS * 1000 / (time + 1)) + " huge OR queries per sec");
    }

    /**
     * Tests the performance of lots of simple queries on the same subject.
     */
    // TODO This is slow. Top N for 100000 assocs currently takes 5ms
    @Test(dependsOnMethods = "testInsert02")
    public void testQuery04() throws QueryExecutionException {
        int ITERATIONS = 1000;

        long before = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            store.query(new Query(Restrictions.phrase("alma")).limit(10));
        }

        long time = System.currentTimeMillis() - before;

        System.err.println((ITERATIONS * 1000 / (time + 1)) +
            " same subject simple queries per sec");
    }
}
