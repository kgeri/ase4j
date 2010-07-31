package examples;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.HTMLConfiguration;
import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.AssociationStoreException;
import org.ogreg.ase4j.file.FileAssociationStoreImpl;
import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.ostore.Configuration;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.StringStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An example of storing indexed associations in the storage.
 * 
 * @author Gergely Kiss
 */
public class WebIndexer {
	private static final Logger log = LoggerFactory.getLogger(WebIndexer.class);

	FileAssociationStoreImpl<String, Document> store = new FileAssociationStoreImpl<String, Document>();
	DefaultHttpClient client;

	Stack<Page> stack = new Stack<Page>();
	int indexed = 0;

	private void start() throws IOException, ObjectStoreException {
		String url = System.getProperty("url");

		if (url == null) {
			throw new IllegalArgumentException("The system property -Durl must be specified");
		}

		String subjectPath = System.getProperty("subjectIndex");
		String docPath = System.getProperty("urlIndex");
		String assocsPath = System.getProperty("assocs");
		int depth = Integer.getInteger("depth", 2);

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

		log.info("Stores initialized. Indexing: {} (depth={})", url, depth);

		client = new DefaultHttpClient();
		stack.push(new Page(url, 0));

		while (!stack.isEmpty()) {
			Page page = stack.pop();

			if (page.level >= depth) {
				continue;
			}

			try {
				index(page.url, page.level);

				log.info("Page indexed successfully: {}", page.url);
			} catch (Exception e) {
				log.error("Failed to index: {} ({})", page.url, e.getLocalizedMessage());
				log.debug("Failure trace", e);
			}
		}

		SerializationUtils.write(subjectFile, fromStore);
		((Flushable) documentStore).flush();
		((Closeable) documentStore).close();
		store.flush();
		store.close();

		log.info("Indexing finished. Indexed {} pages", indexed);
	}

	private void index(String url, int level) throws IOException, IllegalStateException,
			SAXException, AssociationStoreException {
		log.debug("Downloading page: {}", url);

		HttpUriRequest request = new HttpGet(url);
		HtmlParser parser = new HtmlParser(url);

		try {
			HttpResponse response = client.execute(request);

			int status = response.getStatusLine().getStatusCode();

			if (status != 200) {
				throw new IOException("Server returned status " + status + " for URL: " + url);
			}

			parser.parse(new InputSource(response.getEntity().getContent()));
		} finally {
			request.abort();
		}

		for (String childUrl : parser.getUrls()) {
			stack.push(new Page(childUrl, level + 1));
		}

		Document doc = new Document(url);
		StringTokenizer tok = new StringTokenizer(parser.getText().toString(), " -!?,;.()[]|\u00a0");
		Collection<Association<String, Document>> assocs = new LinkedList<Association<String, Document>>();

		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();

			if (token == null) {
				continue;
			}

			token = token.trim().toLowerCase();

			if ((token.length() == 0) || (token.length() > 20)) {
				continue;
			}

			assocs.add(new Association<String, Document>(token, null, 0.5F));
		}

		store.addAll(assocs, doc);

		indexed++;
	}

	public static void main(String[] args) throws Exception {
		new WebIndexer().start();
	}

	class Page {
		final String url;
		final int level;

		public Page(String url, int level) {
			this.url = url;
			this.level = level;
		}
	}

	class HtmlParser extends AbstractSAXParser {
		StringBuilder buf = new StringBuilder();
		Set<String> urls = new HashSet<String>();
		String baseUrl;

		public HtmlParser(String baseUrl) {
			super(new HTMLConfiguration());
			this.baseUrl = baseUrl;
		}

		@Override
		public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
				throws XNIException {

			if ("A".equalsIgnoreCase(element.localpart)) {
				String url = attributes.getValue("href");

				if ((url != null) && !url.contains("#")) {

					if (url.startsWith("http://")) {
						urls.add(url);
					} else {
						urls.add(baseUrl + "/" + url);
					}
				}
			}
		}

		@Override
		public void characters(XMLString text, Augmentations augs) throws XNIException {
			buf.append(text.ch, text.offset, text.length);
		}

		public StringBuilder getText() {
			return buf;
		}

		public Set<String> getUrls() {
			return urls;
		}
	}
}
