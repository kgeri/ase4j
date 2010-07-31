package examples;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.AssociationStore;
import org.ogreg.ase4j.AssociationStoreManager;
import org.ogreg.ase4j.criteria.Expression;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of searching the association storage.
 * 
 * @author Gergely Kiss
 */
public class Search {
	private static final Logger log = LoggerFactory.getLogger(Search.class);

	private void start() throws IOException {
		String query = System.getProperty("query");

		if (query == null) {
			throw new IllegalArgumentException("The system property -Dquery must be specified.");
		}

		String dataPath = System.getProperty("dataDir");
		File dataDir = new File((dataPath == null) ? "target" : dataPath);

		log.info("Initializing stores.");

		AssociationStoreManager cfg = new AssociationStoreManager();
		cfg.setDataDir(dataDir);
		cfg.add("store.xml");

		@SuppressWarnings("unchecked")
		AssociationStore<String, Document> store = cfg.createStore("index");

		log.info("Stores initialized. Searching for: {}", query);
		log.info("Results:");

		String[] queries = query.split("[\\s]+");
		Expression[] exprs = new Expression[queries.length];

		for (int i = 0; i < queries.length; i++) {
			exprs[i] = Restrictions.phrase(queries[i]);
		}

		try {
			List<Association<String, Document>> assocs = store.query(new Query(Restrictions.and(
					exprs[0], exprs)).limit(10));

			for (Association<String, Document> assoc : assocs) {
				log.info("    {} ({})", assoc.to, assoc.value);
			}

			log.info("Search finished.");
		} catch (QueryExecutionException e) {
			log.info("Search failed: {}", e.getLocalizedMessage());
			log.info("Failure trace", e);
		}
	}

	public static void main(String[] args) throws Exception {
		new Search().start();
	}
}
