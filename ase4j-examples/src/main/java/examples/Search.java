package examples;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.AssociationStore;
import org.ogreg.ase4j.StorageClient;
import org.ogreg.ase4j.criteria.Expression;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.Restrictions;

import org.ogreg.common.dynamo.DynamicObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * An example of searching the association storage.
 *
 * @author  Gergely Kiss
 */
public class Search {
    private static final Logger log = LoggerFactory.getLogger(Search.class);

    private void start() throws Exception {
        String query = System.getProperty("query");

        if (query == null) {
            throw new IllegalArgumentException("The system property -Dquery must be specified.");
        }

        log.info("Looking up store...");

        AssociationStore<String, DynamicObject> store = StorageClient.lookupStore(
                "//localhost:1198/assocs/index", String.class, DynamicObject.class);

        log.info("Store found. Searching for: {}", query);
        log.info("Results:");

        String[] queries = query.split("[\\s]+");
        Expression[] exprs = new Expression[queries.length];

        for (int i = 0; i < queries.length; i++) {
            exprs[i] = Restrictions.phrase(queries[i]);
        }

        try {
            long before = System.currentTimeMillis();

            List<Association<String, DynamicObject>> assocs = store.query(
                    new Query(Restrictions.and(exprs[0], exprs)).limit(10));

            long time = System.currentTimeMillis() - before;

            for (Association<String, DynamicObject> assoc : assocs) {
                log.info("    {} ({})", assoc.to.get("url"), assoc.value);
            }

            log.info("Search finished in {} ms.", time);
        } catch (QueryExecutionException e) {
            log.info("Search failed: {}", e.getLocalizedMessage());
            log.info("Failure trace", e);
        }
    }

    public static void main(String[] args) throws Exception {
        new Search().start();
    }
}
