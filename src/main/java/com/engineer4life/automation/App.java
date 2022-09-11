package com.engineer4life.automation;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

public class App
{
    public static void main( String[] args ) {
        try(Playwright playwright = Playwright.create()){
            var launchOpts = new BrowserType.LaunchOptions()
                    .setSlowMo(100)
                    .setHeadless(false);

            var browser = playwright.firefox().launch(launchOpts);
            new TaylorMorrisonHouseTracker(browser).scrape("https://www.taylormorrison.com");
//            new DreesHomes(browser).scrape("https://www.dreeshomes.com/custom-homes/austin/community/clearwater_ranch/clearwater_ranch/");
//            new DreesHomes(browser).scrape("https://www.dreeshomes.com/custom-homes/austin/community/wolf_ranch/wolf_ranch-60/");
        }
    }
}
