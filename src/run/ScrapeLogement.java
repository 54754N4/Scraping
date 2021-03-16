package run;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import scrape.Browser;

public class ScrapeLogement {
	public static void main(String[] args) {
		String url = "https://www.seloger.com/list.htm?projects=2,5&types=1,2&natures=1,2,4&places=[%7B%22inseeCodes%22:[490007]%7D]&proximities=0,10&price=NaN/150000&enterprise=0&qsVersion=1.0&m=search_refine";
		try (Browser browser = new Browser(false)) {
			WebElement element = browser.visit(url)
				.waitGet(By.cssSelector("[data-test*=card-container]"));
			System.out.println(element.getText());
		}
	}
}
