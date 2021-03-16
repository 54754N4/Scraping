package scrape;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PagedResult<T> extends ArrayList<T> {
	private static final long serialVersionUID = 3403186784231334418L;
	
	private int count;
	private String url;
	private LocalDateTime scraped;
	
	protected PagedResult(int count, String url, LocalDateTime scraped, List<T> elements) {
		super(elements);
		this.url = url;
		this.count = count;
		this.scraped = scraped;
	}
	
	public int getCount() {
		return count;
	}
	
	public String getUrl() {
		return url;
	}

	public LocalDateTime getScraped() {
		return scraped;
	}

	public List<T> getElements() {
		return this;
	}

	public abstract static class Builder<Type, Out> {
		protected static int count = 0;	// to number page instances
		protected String url;
		protected LocalDateTime scraped;
		protected List<Type> elements;
		
		protected Builder() {
			elements = new ArrayList<>();
		}
		
		public Builder<Type, Out> setUrl(String url) {
			this.url = url;
			return this;
		}
		
		public Builder<Type, Out> setTime(LocalDateTime scraped) {
			this.scraped = scraped;
			return this;
		}
		
		public Builder<Type, Out> setElements(Collection<Type> elements) {
			this.elements = new ArrayList<>(elements);
			return this;
		}
		
		public Builder<Type, Out> addElement(Type element) {
			elements.add(element);
			return this;
		}
		
		public Builder<Type, Out> addElements(Collection<Type> elements) {
			this.elements.addAll(elements);
			return this;
		}
		
		protected abstract Out buildInstance();
		
		public Out build() {
			count++;
			scraped = scraped == null ? LocalDateTime.now() : scraped;
			try { return buildInstance(); }
			catch (Exception e) { 
				count--;
				throw e;
			}
		}
	}
}
