package run;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

import com.google.gson.internal.LinkedTreeMap;

import target.facebook.Credentials;
import target.facebook.PostsScraper;
import target.facebook.posts.Post;
import target.vnexpress.CommentsScraper;
import target.vnexpress.comments.Comment;

/*  https://jsonformatter.curiousconcept.com/#
 *  for JSON pretty-printing since GSON makes it ugly */
public class ScrapeFacebookPosts {
	public static void main(String[] args) throws Exception {
		Credentials credentials = Credentials.fromFile(Paths.get("src/target/facebook/creds.txt"));
		String[] urls = { "https://www.facebook.com/rmitvnconf/" };
		Predicate<Post> stop = post -> post.getTime().contains("22:22");	// scrapes until time is 22:22
		PostsScraper scraper = new PostsScraper(credentials, stop, urls);
		Path path = scraper.serialize("posts.json");
		// Deserialization
		List<List<LinkedTreeMap<String, Comment>>> pages = CommentsScraper.deserialize(path.toString());
		pages.forEach(page -> page.forEach(System.out::println));
	}
}