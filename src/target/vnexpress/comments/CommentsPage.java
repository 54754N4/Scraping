package target.vnexpress.comments;

import java.time.LocalDateTime;
import java.util.List;

import scrape.PagedResult;

public class CommentsPage extends PagedResult<Comment> {
	private static final long serialVersionUID = 7692137280218198995L;

	private CommentsPage(int count, String url, LocalDateTime scraped, List<Comment> comments) {
		super(count, url, scraped, comments);
	}
	
	public static class Builder extends PagedResult.Builder<Comment, CommentsPage> {
		public Builder() {
			super();
		}
		
		@Override
		public CommentsPage buildInstance() {
			if (url == null || url.equals(""))
				throw new IllegalArgumentException("Invalid page : empty url.");
			return new CommentsPage(count, url, scraped, elements);
		}
	}
}
