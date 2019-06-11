package com.luoheng.example.feizhu;

import com.luoheng.example.util.PropertiesUtil;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ScenicSpotCrawler implements Runnable {
    private static final String RESPONSE_FILE_PATH="./fiddler-feizhu-info.log";
    private static final String TARGET_URL="https://h5.m.taobao.com/trip/rx-search/travel-list/index.html?keyword=wuhan&fromSug=1&_wx_tpl=https%3A%2F%2Fh5.m.taobao.com%2Ftrip%2Frx-search%2Ftravel-list%2Findex.weex.js&globalSearchSource=sug_trip_scenic&nav=SCENIC&spm=181.8512603.x2112542.dHLItem-historyList-0-0&gsclickquery=wuhan&buyerLoc=%E6%AD%A6%E6%B1%89&ttid=seo.000000575&_projVer=0.1.90";
    private int totalPage;
    private int currentpage;
    private ChromeDriver chromeDriver;
    public ScenicSpotCrawler(){
        initSelenium();
    }
    private void initSelenium(){
        System.setProperty("webdriver.chrome.driver", PropertiesUtil.getValue("chromedriver"));
        Map<String, String> mobileEmulation = new HashMap<String, String>();
        mobileEmulation.put("deviceName", "iPhone 6");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("mobileEmulation",mobileEmulation);
        chromeDriver = new ChromeDriver();
        chromeDriver.get(TARGET_URL);
        chromeDriver.manage().timeouts().implicitlyWait(10,TimeUnit.SECONDS);
        getPageFocus(chromeDriver);
        Actions slideActions = new Actions(chromeDriver);
        boolean isDown = true;
        while(isDown){
            //slideActions.sendKeys(Keys.PAGE_DOWN).perform();
        }
    }

    private void getPageFocus(WebDriver webDriver){
        WebElement element = webDriver.findElement(By.xpath("//*[@id=\"1354\"]"));
        Actions getFocus = new Actions(webDriver);
        getFocus.contextClick(element).sendKeys(Keys.ESCAPE).perform();
    }


    private void loadResponse(){
        File file=new File(RESPONSE_FILE_PATH);

    }


    @Override
    public void run() {

    }
    public static void main(String args[]){
        ScenicSpotCrawler scenicSpotCrawler=new ScenicSpotCrawler();
    }
}
