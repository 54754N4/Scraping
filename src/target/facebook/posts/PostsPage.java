package target.facebook.posts;

import java.time.LocalDateTime;
import java.util.List;

import scrape.PagedResult;

public class PostsPage extends PagedResult<Post> {
	private static final long serialVersionUID = -650765447418493318L;

	protected PostsPage(int count, String url, LocalDateTime scraped, List<Post> posts) {
		super(count, url, scraped, posts);
	}

	public static class Builder extends PagedResult.Builder<Post, PostsPage> {
		public Builder() {
			super();
		}
		
		@Override
		public PostsPage buildInstance() {
			if (url == null || url.equals(""))
				throw new IllegalArgumentException("Invalid page : empty url.");
			return new PostsPage(count, url, scraped, elements);
		}
	}
}
