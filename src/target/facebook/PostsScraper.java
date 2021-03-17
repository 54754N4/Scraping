package target.facebook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.google.gson.internal.LinkedTreeMap;

import scrape.Browser;
import scrape.Browser.ScrollOptions.Behavior;
import scrape.Browser.ScrollOptions.Block;
import scrape.Browser.ScrollOptions.Inline;
import scrape.Json;

public class PostsScraper {
	/* Since facebook is fully dynamic, waiting a few seconds after loading a page gives enough lee-way 
	 * for any dynamic changes (e.g. currently running JS code) to finish before we start scraping
	 */
	private static final long INITIAL_PAGE_WAIT = 5*1000;	// in ms 
	private static final String POSTS_SELECTOR_FORMAT = "div:nth-child(2) > div > div[role=main] > div > div:nth-child(%d)";
	private static final By 
		// Login selectors
		USERNAME_SELECTOR = By.id("email"),
		PASSWORD_SELECTOR = By.id("pass"),
		TWOFA_SELECTOR = By.id("approvals_code"),
		SUBMIT_SELECTOR = By.id("checkpointSubmitButton"),
		ACCOUNT_LOGO_SELECTOR = By.cssSelector("html:nth-of-type(1) > body:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(2) > div:nth-of-type(4) > div:nth-of-type(1) > div:nth-of-type(4) > a:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > :nth-of-type(1)"),
		// Article selectors (obfuscated HTML -> so we can only use attributes, hierarchical and children selectors)
		TIME_SELECTOR = By.cssSelector("body > div > div > div[data-pagelet=root] > div > div:nth-child(6) > div > div > div:nth-child(2) > div > div > div > span[role=tooltip] > div > div > span"),
		HOVER_SELECTOR = By.cssSelector("div > div > div > div > div:nth-child(4) > div:nth-child(2) > div > div:nth-child(2) > div > div > div > div > div > div > div > div > div > div > div > div > div > div:nth-child(2) > div > div:nth-child(2) > div > div:nth-child(2) > div > div:nth-child(2) > span > span > span:nth-child(2)"),
		TEXT_SELECTOR = By.cssSelector("div > div > div > div > div:nth-child(4) > div:nth-child(2) > div > div:nth-child(2) > div > div > div > div > div > div > div > div > div > div > div > div > div > div:nth-child(2) > div > div:nth-child(3) > div"),
		LIKES_SELECTOR = By.cssSelector("div > div > div > div > div > div:nth-child(2) > div > div:nth-child(4) > div > div:nth-child(1) > div > div > div > div > div > div:nth-child(2) > span > div > span:nth-child(2)"),
		SHRUNK_MESSAGE_SELECTOR = By.cssSelector("div[role=article] > div > div > div > div > div > div:nth-child(2) > div > div:nth-child(3) > div[data-ad-preview=message] > div > div > span > div:last-child > div:last-child > div[tabindex=\"0\"]")
		;
	
	private Credentials credentials;		// to retrieve login creds
	private Predicate<Post> stopCondition;	// defines when to stop scraping posts
	private List<PostsPage> pages;
	private int count = 0;					// retrieved posts count
	
	public PostsScraper(Credentials credentials, Predicate<Post> stopCondition, String...urls) {
		this.credentials = credentials;
		this.stopCondition = stopCondition;
		pages = new ArrayList<>();
		try (
			Browser browser = new Browser(false);
			Scanner scanner = new Scanner(System.in)
		) {
			login(browser, scanner);
			for (String url : urls)
				pages.add(loadPosts(browser, url));
		}
	}
	
	/* Login process methods */
	
	private void login(Browser browser, Scanner scanner) {
		System.out.println("Logging into account : "+credentials.getUsername());
		try {
			// Enter credentials and click login
			By loginButton = By.cssSelector("form[method=post] > div > button[name=login]");
			browser.visit("http://www.facebook.com")
				.type(credentials.getUsername(), USERNAME_SELECTOR)
				.type(credentials.getPassword(), PASSWORD_SELECTOR)
				.click(loginButton)
				.waitUntilLoaded();
			check2FA(browser, scanner);
		} catch (Exception e) {
			System.out.println("ABORTED: Could not login.");
			e.printStackTrace();
		}
	}
	
	// Checks for and handles two factor authentication
	private void check2FA(Browser browser, Scanner scanner) {
		if (browser.waitGet(TWOFA_SELECTOR) != null) {
			boolean invalid = false;
			do {
				System.out.println(!invalid ? 
					"Requested two-factor authentication code" :
					"Wrong 2FA code, please try again");
				System.out.print("2FA code: ");
				browser.type(scanner.nextLine(), TWOFA_SELECTOR)
					.click(SUBMIT_SELECTOR);
			} while (invalid = invalid2FA(browser));
			System.out.println("Valid 2FA code, proceeding with login");
			checkSaveBrowser(browser);
			checkRecentLoginReviews(browser);
			browser.waitUntilLoaded();
		}
		if (isNotLoggedIn(browser))
			throw new IllegalStateException("Did not login successfully");
		System.out.println("Logged in successfully");
	}
	
	private void checkRecentLoginReviews(Browser browser) {
		if (askedLoginReview(browser)) {
			System.out.println("Requested to review recent logins");
			browser.waitGet(SUBMIT_SELECTOR).click();
			browser.sleep(INITIAL_PAGE_WAIT);
			System.out.println("Clicked on 'confirm' button");
			browser.waitGet(SUBMIT_SELECTOR).click();
			browser.sleep(INITIAL_PAGE_WAIT);
			System.out.println("Clicked on 'This was me' button");
			checkSaveBrowser(browser);	// you have to save browser again if asked to review
		} else
			System.out.println("No 'recent logins review' form detected");
	}
	
	// There's a 'Save Browser' form during log-in
	private void checkSaveBrowser(Browser browser) {
		WebElement saveBrowser = browser.waitGet(SUBMIT_SELECTOR); 
		if (saveBrowser != null) {
			System.out.println("Clicked on 'Save browser' button");
			saveBrowser.click();
			browser.waitUntilLoaded();
		}
	}
	
	/* Posts scraping methods */
	
	private PostsPage loadPosts(Browser browser, String url) {
		System.out.printf("Loading : %s%n", url);
		// Visit page
		browser.visit(url)
			.waitUntilLoaded()
			.sleep(INITIAL_PAGE_WAIT);
		// Keep scraping posts until stop condition
		List<Post> posts = new ArrayList<>();
		String format;
		WebElement element;
		Post post;
		do {
			format = String.format(POSTS_SELECTOR_FORMAT, ++count);
			element = browser.get(By.cssSelector(format));
			posts.add(post = convertToPost(browser, element));
		} while (!stopCondition.test(post));
		System.out.println("Scraped "+count+" posts.");
		count = 0; 		// reset total posts count per url
		return new PostsPage.Builder()
			.setUrl(url)
			.setElements(posts)
			.build();
	}
	
	private Post convertToPost(Browser browser, WebElement dom) {
		WebElement time = dom.findElement(HOVER_SELECTOR),
				likes = dom.findElement(LIKES_SELECTOR),
				text = dom.findElement(TEXT_SELECTOR);
		// Scroll post into view
		browser.scrollIntoView(time, Behavior.AUTO, Block.CENTER, Inline.CENTER);
		// Optionally expand shrunken message
		try {
			dom.findElement(SHRUNK_MESSAGE_SELECTOR).click();
		} catch (Exception e) {}
		// Scrape data
		long htmlGenerationDelay = 2000;
		Post.Builder builder = new Post.Builder()
			.setText(text.getText())
			.setLikes(likes.getText())
			.setTime(browser.hover(time)
				.sleep(htmlGenerationDelay)
				.get(TIME_SELECTOR)
				.getText());
		// Remove mouse from time DOM element (triggers facebook's onmouseleave cleanup code to delete generated HTML)
		browser.hover(text)
			.sleep(htmlGenerationDelay);	// in this case it's for HTML removal delay
		Post post = builder.build();
		loadComments(post, dom);
		return post;
	}
	
	private void loadComments(Post parent, WebElement post) {
		
	}
	
	/* Convenience methods */
	
	/** On successful login, we should see the user's account logo on the 
	 * banner on top of the page; and hence if login failed, then we know 
	 * we can't find it.
	 */
	public static boolean isNotLoggedIn(Browser browser) {
		return browser.get(ACCOUNT_LOGO_SELECTOR) == null;
	}
	
	/** An invalid 2FA code generates a span element with a data-xui-error
	 * attribute that has the value of the error message
	 */
	public static boolean invalid2FA(Browser browser) {
		return browser.get(By.cssSelector("span[data-xui-error]")) != null; 
	}
	
	/** If facebook asks to review recent logins, this selector should return
	 * a single element (since we're matching the form title) and should contain
	 * "Review recent login"
	 * TODO -- Check if language will be a problem (someone else might have it in VN)
	 */
	public static boolean askedLoginReview(Browser browser) {
		List<WebElement> elements = browser.getAll(By.cssSelector("strong[id]"));
		return elements.size() == 1 &&
				elements.get(0)
					.getText()
					.trim()
					.contains("Review recent login");
	}
	
	/* Serialization code */
	
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