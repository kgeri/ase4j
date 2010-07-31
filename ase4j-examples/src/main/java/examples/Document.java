package examples;

/**
 * An indexed, searchable document on the web.
 * 
 * @author Gergely Kiss
 */
public class Document {
	private String url;

	Document() {
	}

	public Document(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "Doc [url=" + url + "]";
	}
}
