package org.ogreg.ase4j;

import org.ogreg.ase4j.GroupedAssociationStore.Group;
import static org.ogreg.ase4j.TestData.data;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.Restrictions;

import org.ogreg.test.FileTestSupport;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Grouped association storage tests.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "correctness")
public class GroupedAssociationStoreImplTest {
    AssociationStoreManager manager;
    GroupedAssociationStore<String, TestData> store;

    @BeforeClass public void init() {
        manager = new AssociationStoreManager();
        manager.add("configuration/test-store.xml");
    }

    /**
     * Tests adding elements to multiple groups of the store.
     */
    @SuppressWarnings("unchecked")
    public void testAdd01() throws Exception {
        File dataDir = FileTestSupport.createTempDir("gstore");
        manager.setDataDir(dataDir);
        store = manager.getGroupedStore("testGroup");

        Group gr = new Group("gr01", 1.0F).set("gr02", 0.5F);

        // Adding simple assoc
        store.add(gr, "a", data("aaa", new Date(0), 10), 1.0F);

        // Adding multiple assocs
        List<Association<String, TestData>> assocs = new ArrayList<Association<String, TestData>>();
        assocs.add(new Association<String, TestData>("b", data("bbb", new Date(0), 20), 1.0F));
        store.addAll(gr, assocs);

        // Adding multiple assocs to different target
        store.addAll(gr, assocs, data("ccc", new Date(0), 20));

        manager.flushStore("testGroup");
        manager.closeStore("testGroup");

        // Start with the basics...
        assertTrue(new File(dataDir, "grp-testGroup/gr01").exists());
        assertTrue(new File(dataDir, "grp-testGroup/gr02").exists());

        // Now test for contents
        List<Association<String, TestData>> r;
        store = manager.getGroupedStore("testGroup");

        // Get from gr01 returns 1.0
        r = store.query(new Group("gr01", 1.0F), new Query(Restrictions.phrase("a")));
        assertEquals(r.size(), 1);
        assertEquals(r.get(0).to.url, "aaa");
        assertEquals(r.get(0).value, 1.0F);

        // Get from gr02 returns 0.5 (multiplier at insertion)
        r = store.query(new Group("gr02", 1.0F), new Query(Restrictions.phrase("a")));
        assertEquals(r.size(), 1);
        assertEquals(r.get(0).to.url, "aaa");
        assertEquals(r.get(0).value, 0.5F);

        // Testing correctness of the third add in gr02
        r = store.query(new Group("gr02", 1.0F), new Query(Restrictions.phrase("b")));
        assertEquals(r.size(), 2);
        assertEquals(r.get(0).to.url, "bbb");
        assertEquals(r.get(0).value, 0.5F);
        assertEquals(r.get(1).to.url, "ccc");
        assertEquals(r.get(1).value, 0.5F);
    }
}
