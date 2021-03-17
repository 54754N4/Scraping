package run;

import java.nio.file.Path;
import java.util.List;

import com.google.gson.internal.LinkedTreeMap;

import target.vnexpress.Comment;
import target.vnexpress.CommentsScraper;

/*  https://jsonformatter.curiousconcept.com/#
 *  for JSON pretty-printing since GSON makes it ugly */
public class ScrapeVNExpress {
	public static void main(String[] args) throws Exception {
		String[] urls = {
			"https://vnexpress.net/5-gio-nha-trang-don-dep-don-tan-tong-thong-4223047.html",
			"https://vnexpress.net/them-mot-nguoi-nhat-o-ha-noi-duong-tinh-ncov-4235646.html"
		};
		CommentsScraper scraper = new CommentsScraper(urls);
		Path path = scraper.serialize("data/comments.json");
		// Deserialisation
		List<List<LinkedTreeMap<String, Comment>>> pages = CommentsScraper.deserialize(path.toString());
		pages.forEach(page -> page.forEach(System.out::println));
	}
}