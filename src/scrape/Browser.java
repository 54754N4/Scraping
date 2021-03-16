package scrape;

import java.io.Closeable;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/* Creates a browser using Selenium that follows the builder pattern
 * to ease with chained calls/actions. Also uses singleton pattern 
 * to reuse default instance, but still allows further browsers to be
 * created (e.g. public constructors).
 * Docs: https://www.selenium.dev/documentation/en/webdriver/   
 */
public class Browser implements Closeable {
	public static final long DEFAULT_TIMEOUT = 15, DEFAULT_POLLING = 5;	// in seconds
	private static final boolean DEFAULT_HEADLESS = true;
	public static final BiConsumer<Integer, Cookie> COOKIE_PRINTER = (i, c) -> System.out.printf("%d. %s%n", i, c.toString());
	private static Browser INSTANCE;			// lazy-loaded through Browser::getInstance
	
	static {
		// Makes sure firefox driver exists or downloads it
		WebDriverManager.firefoxdriver().arch64().setup();
	}

	private RemoteWebDriver driver;
	
	public Browser() {
		this(DEFAULT_HEADLESS);
	}
	
	public Browser(boolean headless) {
		driver = new FirefoxDriver(
			new FirefoxOptions()
				.setHeadless(headless)
				.setAcceptInsecureCerts(true)
				.addArguments(
						"--disable-gpu",
						"--window-size=1920,1200"
				));
	}
	
	@Override
	public void close() {
		kill();
	}
	
	public Browser visit(String url) {
		driver.navigate().to(url);
		return this;
	}
	
	public void kill() {
		driver.quit();
	}
	
	public String getTitle() {
		return driver.getTitle();
	}
	
	public String getCurrentUrl() {
		return driver.getCurrentUrl();
	}
	
	public Dimension getSize() {
		return driver.manage().window().getSize();
	}
	
	public Browser setSize(int width, int height) {
		driver.manage().window().setSize(new Dimension(width, height));
		return this;
	}
	
	public Browser back() {
		driver.navigate().back();
		return this;
	}
	
	public Browser forward() {
		driver.navigate().forward();
		return this;
	}
	
	public Browser refresh() {
		driver.navigate().refresh();
		return this;
	}
	
	public Browser then(Consumer<WebDriver> consumer) {
		consumer.accept(driver);
		return this;
	}
	
	/* Cookie handling */
	
	public Browser deleteAllCookies() {
		driver.manage().deleteAllCookies();
		return this;
	}
	
	public Browser deleteCookie(String name) {
		driver.manage().deleteCookieNamed(name);
		return this;
	}
	
	public Browser deleteCookie(Cookie cookie) {
		driver.manage().deleteCookie(cookie);
		return this;
	}
	
	public Browser addCookie(Cookie cookie) {
		driver.manage().addCookie(cookie);
		return this;
	}
	
	public Set<Cookie> getCookies() {
		return driver.manage().getCookies();
	}
	
	public Cookie getCookie(String name) {
		return driver.manage().getCookieNamed(name);
	}
	
	public Browser forEachCookie(Consumer<Cookie> action) {
		return forEachCookie(always -> true, action);
	}
	
	public Browser forEachCookie(Predicate<Cookie> filter, Consumer<Cookie> action) {
		return forEach(this::getCookies, filter, action);
	}
	
	public Browser forEachCookieIndexed(BiConsumer<Integer, Cookie> action) {
		return forEachIndexed(this::getCookies, always -> true, action);
	}
	
	public Browser forEachCookieIndexed(Predicate<Cookie> filter, BiConsumer<Integer, Cookie> action) {
		return forEachIndexed(this::getCookies, filter, action);
	}
	
	public Browser printCookies() {
		return forEachCookieIndexed(COOKIE_PRINTER);
	}
	
	/* Screenshot handling */
	
	private <T> T screenshot(TakesScreenshot screenshot, OutputType<T> type) {
		return screenshot.getScreenshotAs(type);
	}
	
	public <T> T screenshotFullAs(OutputType<T> type) {
		return screenshotOf(By.tagName("body"), type);
	}
	
	public File screenshotFullAsFile() {
		return screenshotFullAs(OutputType.FILE);
	}
	
	public File screenshotAsFile() {
		return screenshotAs(OutputType.FILE);
	}
	
	public File screenshotFileOf(By by) {
		return screenshotOf(by, OutputType.FILE);
	}
	
	public <T> T screenshotAs(OutputType<T> type) {
		return screenshot(driver, type);
	}
	
	public <T> T screenshotOf(By by, OutputType<T> type) {
		return screenshot(driver.findElement(by), type);
	}
	
	/* Wait/delays handling */
	
	public WebElement get(By by) {
		try {
			return driver.findElement(by);
		} catch (Exception e) {
			return null;
		}
	}
	
	public List<WebElement> getAll(By by) {
		try {
			return driver.findElements(by);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
	
	public WebElement waitGet(By by) {
		return waitUntil(driver -> driver.findElement(by));
	}
	
	public List<WebElement> waitGetAll(By by) {
		return waitUntil(driver -> driver.findElements(by));
	}
	
	public <V> V waitUntil(Function<? super RemoteWebDriver, V> isTrue) {
		try {
			return new FluentWait<>(driver)
				.until(isTrue);
		} catch (Exception e) {
			return null;
		}
	}
	
	private Browser handle(WebElement element, Collection<Consumer<WebElement>> consumers) {
		for (Consumer<WebElement> consumer : consumers)
			consumer.accept(element);
		return this;
	}
	
	public Browser waitFor(Function<? super WebDriver, Boolean> isTrue, long timeout) {
		new WebDriverWait(driver, timeout)
			.until(isTrue);
		return this;
	}
	
	public Browser waitFor(Function<? super WebDriver, Boolean> isTrue) {
		return waitFor(isTrue, DEFAULT_TIMEOUT);
	}
	
	public Browser waitFor(By by, long timeout, Collection<Consumer<WebElement>> consumers) {
		WebElement element = new WebDriverWait(driver, timeout)
				.until(driver -> driver.findElement(by));
		return handle(element, consumers);
	}
	
	public Browser waitFor(By by, long timeout, long polling, Collection<Class<? extends Throwable>> exceptions, Collection<Consumer<WebElement>> consumers) {
		WebElement element = new FluentWait<>(driver)
				.withTimeout(Duration.ofSeconds(timeout))
				.pollingEvery(Duration.ofSeconds(polling))
				.ignoreAll(exceptions)
				.until(driver -> driver.findElement(by));
		return handle(element, consumers);
	}
	
	public Browser waitFor(By by, long timeout, long polling, Class<? extends Throwable> exception, Collection<Consumer<WebElement>> actions) {
		return waitFor(by, timeout, polling, Arrays.asList(exception), actions);
	}
	
	public Browser waitFor(By by, long timeout, long polling, Collection<Consumer<WebElement>> actions) {
		return waitFor(by, timeout, polling, NoSuchElementException.class, actions);
	}
	
	public Browser waitFor(By by, Collection<Consumer<WebElement>> actions) {
		return waitFor(by, DEFAULT_TIMEOUT, actions);
	}
	
	public Browser waitFor(By by, long timeout, long polling, Class<? extends Throwable> exception) {
		return waitFor(by, timeout, polling, Arrays.asList(exception), Arrays.asList());
	}
	
	public Browser waitFor(By by, long timeout, long polling) {
		return waitFor(by, timeout, polling, NoSuchElementException.class, Arrays.asList());
	}
	
	public Browser waitFor(By by) {
		return waitFor(by, DEFAULT_TIMEOUT, Arrays.asList());
	}
	
	public Browser waitUntilLoaded() {
		return waitUntilLoaded(DEFAULT_TIMEOUT);
	}
	
	public Browser waitUntilLoaded(long seconds) {
		new WebDriverWait(driver, seconds)
			.until(pageLoadedCondition());
		return this;
	}
	
	public final ExpectedCondition<Boolean> pageLoadedCondition() {
		return d -> jsExecutor()
			.executeScript("return document.readyState;")
			.equals("complete");
	}
	
	/* User handling + simulated actions */
	
	public JavascriptExecutor jsExecutor() {
		return (JavascriptExecutor) driver;
	}
	
	public Browser execute(String code, Object...args) {
		jsExecutor().executeScript(code, args);
		return this;
	}
	
	public Actions actions() {
		return new Actions(driver);
	}
	
	public Browser pause() {
		System.out.print("Press any key and <enter> to continue.. ");
		try (Scanner scanner = new Scanner(System.in)) {
			scanner.next();
		} catch (Exception e) {} // ignore
		return this;
	}
	
	public Browser type(final String input, By by) {
		waitFor(by, Arrays.asList(element -> element.sendKeys(input)));
		return this;
	}
	
	public Browser click(By...bys) {
		return click(null, bys);
	}
	
	public Browser click(@Nullable Consumer<Throwable> onError, By...bys) {
		for (By by : bys)
			try { 
				waitGet(by).click(); 
			} catch (Exception e) {
				if (onError != null)
					onError.accept(e);
			}
		return this;
	}
	
	public Browser scrollTo(By target) {
		return scrollTo(target, DEFAULT_TIMEOUT);
	}
	
	public Browser scrollTo(By target, long timeout) {
		WebElement element = new WebDriverWait(driver, timeout)
			.until(ExpectedConditions.presenceOfElementLocated(target));
		return scrollTo(element);
	}
	
	public Browser scrollTo(WebElement element) {
		actions()
			.moveToElement(element)
			.perform();
		return this;
	}
	
	public Browser scrollTo(int x, int y) {
		Point window = driver.manage().window().getPosition();
		return scrollBy(x - window.x, y - window.y);
	}
	
	public Browser scrollBy(int x, int y) {
		actions()
			.moveByOffset(x, y)
			.perform();
		return this;
	}
	
	public Browser scrollToJS(By target) {
		return scrollToJS(target, DEFAULT_TIMEOUT);
	}
	
	public Browser scrollToJS(By target, long timeout) {
		WebElement element = new WebDriverWait(driver, timeout)
				.until(ExpectedConditions.presenceOfElementLocated(target));
		return scrollToJS(element);
	}
	
	public Browser scrollToJS(WebElement element) {
		Point location = element.getLocation();
		return scrollToJS(location.x, location.y);
	}
	
	public Browser scrollToJS(int x, int y) {
		return execute("window.scrollTo(arguments[0], arguments[1]);", x, y);
	}
	
	public Browser scrollByJS(int x, int y) {
		return execute("window.scrollBy(arguments[0], arguments[1]);", x, y);
	}
	
	public Browser scrollAxisBy(boolean abscissa, int delta) {
		String offset = ""+delta,
			code = String.format("window.scrollBy(%s,%s);", 
					abscissa ? offset : "window.scrollX",
					abscissa ? "window.scrollY" : offset);
		return execute(code);
	}
	
	public Browser scrollHorizontallyBy(int dx) {
		return scrollAxisBy(true, dx);
	}
	
	public Browser scrollVerticallyBy(int dy) {
		return scrollAxisBy(false, dy);
	}
	
	public Browser scrollIntoView(By target) {
		WebElement element = new WebDriverWait(driver, DEFAULT_TIMEOUT)
			.until(ExpectedConditions.presenceOfElementLocated(target));
		return scrollIntoView(element);
	}
	
	public Browser scrollIntoView(
			By target, 
			ScrollOptions.Behavior behavior, 
			ScrollOptions.Block block, 
			ScrollOptions.Inline inline) {
		WebElement element = new WebDriverWait(driver, DEFAULT_TIMEOUT)
				.until(ExpectedConditions.presenceOfElementLocated(target));
		return scrollIntoView(element, behavior, block, inline);
	}
	
	public Browser scrollIntoView(WebElement element) {
		return execute("arguments[0].scrollIntoView();", element);
	}
	
	public Browser scrollIntoView(
			WebElement element, 
			ScrollOptions.Behavior behavior, 
			ScrollOptions.Block block, 
			ScrollOptions.Inline inline) {
		String code = String.format("arguments[0].scrollIntoView({behavior: '%s', block: '%s', inline: '%s'});", 
				ScrollOptions.getValue(behavior),
				ScrollOptions.getValue(block),
				ScrollOptions.getValue(inline));
		return execute(code, element);
	}
	
	public Browser hover(WebElement element) {
		actions()
			.moveToElement(element)
			.perform();
		return this;
	}
	
	public Browser hover(By target) {
		return hover(new WebDriverWait(driver, DEFAULT_TIMEOUT)
				.until(ExpectedConditions.presenceOfElementLocated(target)));
	}
	
	public Browser hoverJS(By target) {
		String code = "var evObj = document.createEvent('MouseEvents');" +
                "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);" +
                "arguments[0].dispatchEvent(evObj);";
		WebElement element = new WebDriverWait(driver, DEFAULT_TIMEOUT)
				.until(ExpectedConditions.presenceOfElementLocated(target));
		return execute(code, element);
	}
	
	public Browser sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			System.err.println("Could not sleep for "+millis+"ms");
		}
		return this;
	}
	
	public Browser implicitSleep(long millis) {
		driver.manage().timeouts().implicitlyWait(millis, TimeUnit.MILLISECONDS);
		return this;
	}
	
	/* Convenience methods */
	
	public <T> Browser forEach(Supplier<Collection<T>> supplier, Predicate<T> filter, Consumer<T> action) {
		for (T element : supplier.get())
			if (filter.test(element))
				action.accept(element);
		return this;
	}
	
	public <T> Browser forEachIndexed(Supplier<Collection<T>> supplier, Predicate<T> filter, BiConsumer<Integer, T> action) {
		int index = 0;
		for (T element : supplier.get())
			if (filter.test(element))
				action.accept(index++, element);
		return this;
	}
	
	/* Static methods to act on default singleton instance */
	
	public static Browser restart() {
		if (INSTANCE != null) 
			INSTANCE.kill();
		return INSTANCE = new Browser();
	}
	
	public static Browser getInstance() {
		if (INSTANCE == null)
			INSTANCE = new Browser();
		return INSTANCE;
	}
	
	public static class ScrollOptions {
		public static enum Behavior { SMOOTH, AUTO }
		public static enum Block { START, CENTER, END, NEAREST }
		public static enum Inline { START, CENTER, END, NEAREST }
		
		public static <E extends Enum<E>> String getValue(Enum<E> e) {
			return e.name().toLowerCase();
		}
	}
}
