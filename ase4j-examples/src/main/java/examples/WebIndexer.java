package examples;

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
import org.ogreg.ase4j.AssociationStore;
import org.ogreg.ase4j.AssociationStoreException;
import org.ogreg.ase4j.AssociationStoreManager;
import org.ogreg.ase4j.file.FileAssociationStoreImpl;

import org.ogreg.common.dynamo.DynamicObject;
import org.ogreg.common.dynamo.DynamicType;

import org.ogreg.ostore.ObjectStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;


/**
 * An example of storing indexed associations in the storage.
 *
 * @author  Gergely Kiss
 */
public class WebIndexer {
    private static final Logger log = LoggerFactory.getLogger(WebIndexer.class);
    DefaultHttpClient client;

    AssociationStore<String, DynamicObject> store;
    Stack<Page> stack = new Stack<Page>();
    int indexed = 0;

    @SuppressWarnings("unchecked")
    private void start() throws IOException, ObjectStoreException {
        String url = System.getProperty("url");

        if (url == null) {
            throw new IllegalArgumentException("The system property -Durl must be specified");
        }

        int depth = Integer.getInteger("depth", 2);

        String dataPath = System.getProperty("dataDir");
        File dataDir = new File((dataPath == null) ? "target" : dataPath);

        log.info("Initializing stores.");

        AssociationStoreManager cfg = new AssociationStoreManager();
        cfg.setDataDir(dataDir);
        cfg.add("store.xml");

        store = cfg.getStore("index");

        log.info("Stores initialized. Indexing: {}", url);

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

        // TODO Flush strategy
        @SuppressWarnings("rawtypes")
        FileAssociationStoreImpl s = (FileAssociationStoreImpl) store;
        s.flush();
        s.getFromStore().flush();
        s.getToStore().flush();

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

        DynamicType type = store.getMetadata().getToMetadata().getDynamicType();

        DynamicObject doc = new DynamicObject(type);
        doc.set("url", url);

        StringTokenizer tok = new StringTokenizer(parser.getText().toString(),
                " -!?,;.()[]|\u00a0");
        Collection<Association<String, DynamicObject>> assocs =
            new LinkedList<Association<String, DynamicObject>>();

        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();

            if (token == null) {
                continue;
            }

            token = token.trim().toLowerCase();

            if ((token.length() == 0) || (token.length() > 20)) {
                continue;
            }

            // 'to' is null, because we'll use addAll for doc
            assocs.add(new Association<String, DynamicObject>(token, null, 0.5F));
        }

        store.addAll(assocs, doc, null);

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

        @Override public void startElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {

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

        @Override public void characters(XMLString text, Augmentations augs) throws XNIException {
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
