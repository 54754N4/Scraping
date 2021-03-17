package target.facebook;

import java.util.ArrayList;
import java.util.List;

public class Post {
	public String text, time, likes;	// public just for gson
	public List<Comment> replies;

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
	
	public Post addReply(Comment reply) {
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
			if (text == null)
				throw new IllegalArgumentException("Text cannot be null");
			else if (time == null)
				throw new IllegalArgumentException("Time cannot be null");
			return new Post(text, time, likes);
		}
	}
	
	public static class Comment {
		public String user, text, time, reactions;
		
		private Comment(String user, String text, String time, String reactions) {
			this.user = user;
			this.text = text;
			this.time = time;
			this.reactions = reactions;
		}

		public String getUser() {
			return user;
		}

		public String getText() {
			return text;
		}

		public String getTime() {
			return time;
		}

		public String getReactions() {
			return reactions;
		}
		
		public static class Builder {
			private String user, text, time, reactions;
			
			public Builder setText(String text) {
				this.text = text;
				return this;
			}

			public Builder setTime(String time) {
				this.time = time;
				return this;
			}

			public Builder setUser(String user) {
				this.user = user;
				return this;
			}

			public Builder setReactions(String reactions) {
				this.reactions = reactions;
				return this;
			}
			
			public Comment build() {
				if (user == null)
					throw new IllegalArgumentException("User cannot be null");
				if (text == null)
					throw new IllegalArgumentException("Text cannot be null");
				if (time == null)
					throw new IllegalArgumentException("Time cannot be null");
				if (reactions == null)
					throw new IllegalArgumentException("Reactions cannot be null");
				return new Comment(user, text, time, reactions);
			}
		}
	}
}
