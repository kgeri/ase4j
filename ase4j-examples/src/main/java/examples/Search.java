package examples;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.criteria.Expression;
import org.ogreg.ase4j.criteria.Restrictions;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.file.FileAssociationStoreImpl;
import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.ostore.Configuration;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.StringStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of searching the association storage.
 * 
 * @author Gergely Kiss
 */
public class Search {
	private static final Logger log = LoggerFactory.getLogger(Search.class);

	FileAssociationStoreImpl<Document> store = new FileAssociationStoreImpl<Document>();

	private void start() throws IOException, ObjectStoreException {
		String query = System.getProperty("query");

		if (query == null) {
			throw new IllegalArgumentException("The system property -Dquery must be specified.");
		}

		String subjectPath = System.getProperty("subjectIndex");
		String docPath = System.getProperty("urlIndex");
		String assocsPath = System.getProperty("assocs");

		File subjectFile = new File((subjectPath == null) ? "target/subjects" : subjectPath);
		File documentDir = new File((docPath == null) ? "target/urls" : docPath);
		File assocsFile = new File((assocsPath == null) ? "target/webassocs" : assocsPath);

		log.info("Initializing index stores.");

		Configuration cfg = new Configuration();
		cfg.add("ostore.xml");

		StringStore fromStore = SerializationUtils.read(subjectFile, StringStore.class);
		ObjectStore<Document> documentStore = cfg.createStore(Document.class, documentDir);

		store.setFromStore(fromStore);
		store.setToStore(documentStore);
		store.setStorageFile(assocsFile);
		store.init();

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
