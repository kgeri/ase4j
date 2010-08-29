package org.ogreg.util;

import org.ogreg.test.Benchmark;
import org.ogreg.test.Benchmark.Result;
import org.ogreg.test.TestUtils;
import org.ogreg.util.btree.BTree;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Benchmarks for tries.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "performance")
public class TrieBenchmark {
    int ITERATIONS = 500000;
    List<String> words = TestUtils.randomWords(ITERATIONS, 31);

    /**
     * Tests the speed of inserts and searches of random words in a hash map.
     */
    public void testHashMap() {
        long cnt = 0;
        resetHashCache(words);

        HashMap<String, Long> map;

        {
            Benchmark.start();
            map = new HashMap<String, Long>();

            for (String word : words) {
                map.put(word, cnt++);
            }

            Result r = Benchmark.stop();

            System.err.printf("HashMap %d puts in: %d ms using %d Kb mem\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
        }

        resetHashCache(words);

        {
            Benchmark.start();

            for (String word : words) {
                map.get(word);
            }

            Result r = Benchmark.stop();

            System.err.printf("HashMap %d gets in: %d ms\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Tests the speed of inserts and searches in a trie.
     */
    public void testTrie() {
        long cnt = 0;
        resetHashCache(words);

        Trie<Long> trie;

        {
            Benchmark.start();
            trie = new Trie<Long>();

            for (String word : words) {
                trie.set(word, cnt++);
            }

            Result r = Benchmark.stop();

            System.err.printf("Trie %d puts in: %d ms using %d Kb mem\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
        }

        {
            Benchmark.start();

            for (String word : words) {
                trie.get(word);
            }

            Result r = Benchmark.stop();

            System.err.printf("Trie %d gets in: %d ms\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS));
            // try {
            // Thread.sleep(100000);
            // } catch (InterruptedException e) {
            // }
        }
    }

    /**
     * Tests the speed of inserts and searches in a trie.
     */
    public void testIntTrie() {
        int cnt = 0;
        resetHashCache(words);

        IntTrie inttrie;

        {
            Benchmark.start();
            inttrie = new IntTrie();

            for (String word : words) {
                inttrie.set(word, cnt++);
            }

            Result r = Benchmark.stop();

            System.err.printf("IntTrie %d puts in: %d ms using %d Kb mem\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
        }

        {
            Benchmark.start();

            for (String word : words) {
                inttrie.get(word);
            }

            Result r = Benchmark.stop();

            System.err.printf("IntTrie %d gets in: %d\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Tests the performance of the insert operation.
     */
    public void testBTree() {
        BTree bt;
        int cnt = 0;

        {
            Benchmark.start();
            bt = new BTree(1024);

            for (String word : words) {
                bt.set(word, cnt++);
            }

            Result r = Benchmark.stop();

            System.err.printf("BTree %d puts in: %d ms using %d Kb mem\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
        }

        {
            Benchmark.start();
            bt = new BTree(1024);

            for (String word : words) {
                bt.get(word);
            }

            Result r = Benchmark.stop();

            System.err.printf("BTree %d gets in: %d ms using %d Kb mem\n", ITERATIONS,
                r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
        }
    }

    private void resetHashCache(List<String> words) {
        Field hf;

        try {
            hf = String.class.getDeclaredField("hash");
            hf.setAccessible(true);

            for (String word : words) {
                hf.setInt(word, 0);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
