package target.vnexpress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.google.gson.internal.LinkedTreeMap;

import scrape.Browser;
import scrape.Json;

public class CommentsScraper {
	public static final String USER_SELECTOR = "a.nickname > b",
			BIG_USER_SELECTOR = ".content_more > .txt-name > .nickname > b",
			MESSAGE_SELECTOR = "div.content-comment > p.full_content",
			BIG_MESSAGE_SELECTOR = "div.content-comment > p.content_more",
			TIME_SELECTOR = "span.time-com",
			LOAD_SELECTOR = "p.count-reply > a.view_all_reply",
			SHRUNKEN_COMMENT = "div.content-comment > p.content_less > a.icon_show_full_comment";
	private int count = 0;	// comments count
	private List<CommentsPage> pages;
	
	public CommentsScraper(String...urls) {
		pages = new ArrayList<>();
		try (Browser browser = new Browser(true)) {
			for (String url : urls)
				pages.add(loadComments(browser, url));
		}
	}
	
	private CommentsPage loadComments(Browser browser, String url) {
		System.out.printf("Scraping website: %s%n", url);
		WebElement commentsBox = browser
				.visit(url)	// load website
				.waitGet(By.cssSelector("div.box_comment_vne.width_common"));
		// Click on 'Xem them' first to load everything
		WebElement xemThem = commentsBox.findElement(By.className("view_more_coment"));
		if (xemThem != null)	// if there's no button we don't care
			xemThem.click();
		// Get all comments
		List<Comment> comments = commentsBox.findElements(By.cssSelector("div.comment_item.width_common"))
			.stream()
			.map(this::convertToComment)
			.collect(Collectors.toList());
		System.out.printf("Global comments : %d%n", comments.size());
		System.out.printf("Total comments : %d%n", count);
		count = 0;	// reset total page comments count
		return new CommentsPage.Builder()
			.setUrl(url)
			.setElements(comments)
			.build();
	}
	
	private Comment convertToComment(WebElement domComment) {
		++count;
		// Expand if big comment
		boolean shrunk;
		try {
			domComment.findElement(By.cssSelector(SHRUNKEN_COMMENT)).click();
			shrunk = true;  // expand button exists
		} catch (Exception e) { 
			shrunk = false; 
		}
		Comment.Builder builder = new Comment.Builder();
		String user = domComment.findElement(By.cssSelector(shrunk ? BIG_USER_SELECTOR : USER_SELECTOR))
					.getText();
		builder.setUser(user);
		String message = domComment.findElement(By.cssSelector(shrunk ? BIG_MESSAGE_SELECTOR : MESSAGE_SELECTOR))
				.getText()
				.substring(user.length());
		builder.setMessage(message);
		builder.setTimestamp(domComment.findElement(By.cssSelector(TIME_SELECTOR)).getText());
		Comment comment = builder.build();
		loadMore(comment, domComment);
		return comment;
	}
	
	private void loadMore(Comment parent, WebElement comment) {
		// Load all replies until load button disappears
		boolean stop = false;
		while (!stop) {
			try { 
				comment.findElement(By.cssSelector(LOAD_SELECTOR)).click();
			} catch (Exception e) {		// NoSuchElement
				stop = true;
			}
		}
		// Add replies to parent comment
		comment.findElements(By.cssSelector(".sub_comment_item.comment_item.width_common"))
			.stream()
			.map(this::convertToComment)
			.forEach(parent::addReply);
	}
	
	public Path serialize(String filename) throws IOException {
		return Files.writeString(Paths.get(filename), Json.of(pages));
	}
	
	public static <T> List<List<LinkedTreeMap<String, T>>> deserialize(String filename) throws IOException {
		String json = Files.newBufferedReader(Paths.get(filename))
			.lines()
			.reduce((s1,s2) -> s1 + System.lineSeparator() + s2)
			.get();
		return Json.toList(json);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(pages.toArray());
	}
}