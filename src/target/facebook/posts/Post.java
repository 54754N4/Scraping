package target.facebook.posts;

import java.util.ArrayList;
import java.util.List;

public class Post {
	public String text, time, likes;
	public List<Post> replies;

	private Post(String text, String time, String likes) {
		this.text = text;
		this.time = time;
		this.likes = likes;
		replies = new ArrayList<>();
	}

	public String getText() {
		return text;
	}

	public String getTime() {
		return time;
	}
	
	public String getLikes() {
		return likes;
	}
	
	public Post addReply(Post reply) {
		replies.add(reply);
		return this;
	}
	
	@Override
	public String toString() {
		return String.format("Text: %s%n" +
			"Time: %s%n" +
			"Likes: %s%n",
			text,
			time,
			likes);
	}
	
	public static class Builder {
		private String text, time, likes;

		public Builder setText(String text) {
			this.text = text;
			return this;
		}

		public Builder setTime(String time) {
			this.time = time;
			return this;
		}
		
		public Builder setLikes(String likes) {
			this.likes = likes;
			return this;
		}
		
		public Post build() {
//			if (text == null)
//				throw new IllegalArgumentException("Text cannot be null");
//			else if (time == null)
//				throw new IllegalArgumentException("Time cannot be null");
			return new Post(text, time, likes);
		}
	}
}
