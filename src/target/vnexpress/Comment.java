package target.vnexpress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Comment {
	public final String user, message, timestamp;
	public final List<Comment> replies;
	
	private Comment(String user, String message, String timestamp) {
		this.user = user;
		this.message = message;
		this.timestamp = timestamp;
		this.replies = new ArrayList<>();
	}
	
	public Comment addReply(Comment reply) {
		replies.add(reply);
		return this;
	}
	
	@Override
	public String toString() {
		return jsonify(0);
	}
	
	public static class Builder {
		public String user, message, timestamp;
	
		public Comment.Builder setUser(String user) {
			this.user = user;
			return this;
		}
		
		public Comment.Builder setMessage(String message) {
			this.message = message;
			return this;
		}
		
		public Comment.Builder setTimestamp(String timestamp) {
			this.timestamp = timestamp;
			return this;
		}
		
		public Comment build() {
			if (user == null)
				throw new IllegalArgumentException("User cannot be null");
			if (message == null)
				throw new IllegalArgumentException("Message cannot be null");
			if (timestamp == null)
				throw new IllegalArgumentException("Timestamp cannot be null");
			return new Comment(user, message, timestamp);
		}
	}
	
	// Manual jsonify methods
	
	private static String escape(String input) {
		return input.replace("\"", "\\\"")	// escape double quotes
				.replace("\n", "\\n");		// escape newlines
	}
	
	private static String tabs(int i) {
		StringBuilder sb = new StringBuilder();
		while (i-->0) sb.append("\t");
		return sb.toString();
	}
	
	private String jsonify(final int depth) {
		final String tab = tabs(depth);
		return  "\n"+
				tab + "{\n" +
				tab + "\t\"user\": \"" + user + "\",\n" +
				tab + "\t\"message\": \"" + escape(message) + "\",\n" +
				tab + "\t\"timestamp\": \"" + timestamp + "\",\n" +
				tab + "\t\"replies\": " + Arrays.deepToString(replies.stream()
					.map(comment -> comment.jsonify(depth+1))
					.collect(Collectors.toList())
					.toArray()) + "\n" +
				tab + "}";
	}
}