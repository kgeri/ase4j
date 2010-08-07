package org.ogreg.ase4j.benchmark;

import org.ogreg.common.dynamo.DynamicObject;
import org.ogreg.common.dynamo.DynamicType;
import org.ogreg.common.utils.MemoryUtils;

import org.ogreg.util.IntSelector;

import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * Various performance improvement tryouts.
 *
 * @author  Gergely Kiss
 */
@Test public class PerformanceChecks {

    private enum Type {
        TEST, TEST2;
    }

    @SuppressWarnings("unused")
    public void testArrayVsTypeCreationSpeed() throws Exception {
        long before, time1, time2;

        before = System.currentTimeMillis();

        for (int i = 0; i < 10000000; i++) {
            Object[] o = new Object[3];
        }

        time1 = System.currentTimeMillis() - before;

        System.gc();

        before = System.currentTimeMillis();

        for (int i = 0; i < 10000000; i++) {
            Assoc<Object, Object> o = new Assoc<Object, Object>();
        }

        time2 = System.currentTimeMillis() - before;

        System.err.printf("Array: %d, Type: %d, Speedup: %d%%\n", time1, time2,
            (time1 - time2) * 100 / time1);

        // Type WIN
    }

    public void testArraySortVsIntSelector() {
        int[] huge = new int[10000000];
        Random r = new Random();

        for (int i = 0; i < huge.length; i++) {
            huge[i] = r.nextInt(huge.length);
        }

        {
            long before;
            before = System.currentTimeMillis();

            IntSelector selector = new IntSelector(1000);

            for (int i = 0; i < huge.length; i++) {
                selector.add(i, huge[i]);
            }

            System.err.println("SELECTOR: " + (System.currentTimeMillis() - before) + " ms");
        }

        {
            long before;
            before = System.currentTimeMillis();

            Arrays.sort(huge);

            System.err.println("SORT: " + (System.currentTimeMillis() - before) + " ms");
        }
        // IntSelector EPIC WIN
    }

    @Test public void testBooleanVsBitSet() {
        gc("start");

        {
            BitSet bs = new BitSet(10 * 1024 * 1024);
            System.err.println("BitSet of 10M");
            gc("bitset");

            long before = System.currentTimeMillis();

            for (int i = 0; i < bs.size(); i++) {
                bs.set(i);
            }

            System.err.println("BitSet: " + (System.currentTimeMillis() - before) + " ms");
        }

        {
            boolean[] bool = new boolean[10 * 1024 * 1024];
            System.err.println("Boolean array of 10M");
            gc("boolean array");

            long before = System.currentTimeMillis();

            for (int i = 0; i < bool.length; i++) {
                bool[i] = true;
            }

            System.err.println("Boolean array: " + (System.currentTimeMillis() - before) + " ms");
        }
        // BitSet WIN
    }

    @Test public void testInstanceOfVsGetType() {
        TestInterface[] dummy = new TestInterface[2];
        dummy[0] = new TestType();
        dummy[1] = new TestType2();

        TestInterface test = dummy[0];

        Random r = new Random(0);

        {
            long before = System.currentTimeMillis();

            for (int i = 0; i < 100000000; i++) {

                if (test instanceof TestType) {
                    test = dummy[r.nextInt(2)];
                }
            }

            System.err.println("InstanceOf: " + (System.currentTimeMillis() - before) + " ms");
        }

        {
            long before = System.currentTimeMillis();

            for (int i = 0; i < 100000000; i++) {

                if (test.getEnumType() == Type.TEST) {
                    test = dummy[r.nextInt(2)];
                }
            }

            System.err.println("GetType enum: " + (System.currentTimeMillis() - before) + " ms");
        }

        {
            long before = System.currentTimeMillis();

            for (int i = 0; i < 100000000; i++) {

                if (test.getType() == 0) {
                    test = dummy[r.nextInt(2)];
                }
            }

            System.err.println("GetType int: " + (System.currentTimeMillis() - before) + " ms");
        }
        // At an impressive 100M cycles: instanceof > x30 > enum == int
        // instanceof seems to get slower, while the other two are constant (JIT
        // optimizations may have kicked in)
        // Also it seems the performance penalty of an instanceof is 10^-6 ms.
    }

    @Test public void testTypeVsMapVsCustom() throws Exception {
        int ITERATIONS = 10000000;
        Object[] dummy = new Object[ITERATIONS / 1000];
        String url = "testurl";
        Date date = new Date();
        long size = 1000;

        gc("start");

        long before, time1, time2, time3;

        {
            Map<String, Field> fields = new HashMap<String, Field>();

            for (Field field : TestDataType.class.getDeclaredFields()) {
                field.setAccessible(true);
                fields.put(field.getName(), field);
            }

            Constructor<TestDataType> ctor = TestDataType.class.getConstructor();
            ctor.setAccessible(true);

            before = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                Object data = ctor.newInstance();
                fields.get("url").set(data, url);
                fields.get("date").set(data, date);
                fields.get("size").set(data, size);

                dummy[i % dummy.length] = data;
            }

            time1 = System.currentTimeMillis() - before;
        }

        gc("type");

        {
            before = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("url", url);
                map.put("date", date);
                map.put("size", size);
                dummy[i % dummy.length] = map;
            }

            time2 = System.currentTimeMillis() - before;
        }

        gc("map");

        {
            Map<String, Class<?>> props = new HashMap<String, Class<?>>();
            props.put("url", String.class);
            props.put("date", Date.class);
            props.put("size", Long.class);

            DynamicType type = DynamicType.define("test", props);

            before = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                DynamicObject dobj = new DynamicObject(type);
                dobj.set("url", url);
                dobj.set("date", date);
                dobj.set("size", size);
                dummy[i % dummy.length] = dobj;
            }

            time3 = System.currentTimeMillis() - before;
        }

        gc("custom");

        System.err.printf("Type: %d, Map: %d, Custom: %d\n", time1, time2, time3);
        // Custom WIN with about 15%
    }

    void gc(String message) {
        System.gc();
        System.err.println("After " + message + ": " + (MemoryUtils.usedMem() / 1024) + "k");
        System.gc();
    }

    private interface TestInterface {
        int getType();

        Type getEnumType();
    }

    @SuppressWarnings("unused")
    private static final class Assoc<S, T> {
        S source;
        T target;
        float value;
    }

    private static class TestType extends ArrayList<String> implements TestInterface {
        private static final long serialVersionUID = 3265053284420914382L;

        @Override public int getType() {
            return 0;
        }

        @Override public Type getEnumType() {
            return Type.TEST;
        }
    }

    private static class TestType2 extends ArrayList<String> implements TestInterface {
        private static final long serialVersionUID = -861637136134087272L;

        @Override public int getType() {
            return 1;
        }

        @Override public Type getEnumType() {
            return Type.TEST2;
        }
    }

    @SuppressWarnings("unused")
    private static final class TestDataType {
        String url;
        Date date;
        long size;

        public TestDataType() {
        }
    }
}
