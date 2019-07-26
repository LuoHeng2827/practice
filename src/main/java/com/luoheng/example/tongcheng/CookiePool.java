package com.luoheng.example.tongcheng;

import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;
import java.util.List;

public class CookiePool extends Thread{
    private int num;
    private boolean complete=false;
    List<Thread> threadList=new ArrayList<>();
    private Logger logger=LogManager.getLogger(CookiePool.class);
    public CookiePool(int num){
        this.num=num;
        init();
    }
    private void init(){
        for(int i=0;i<num;i++){
            MyThread thread=new MyThread(i);
            threadList.add(thread);
            thread.start();
        }
    }


    @Override
    public void run(){
        while(!complete){
            for(int i=0;i<threadList.size();i++){
                Thread thread=threadList.get(i);
                if(!thread.isAlive()){
                    threadList.remove(i);
                    MyThread myThread=new MyThread(i);
                    threadList.add(i,myThread);
                }
            }
            ThreadUtil.waitSecond(20);
            //logger.info(Arrays.asList(Core.cookies).toString());
        }
    }

    public void setComplete(boolean complete){
        this.complete=complete;
    }


    class MyThread extends Thread{
        int number;
        MyThread(int number){
            this.number=number;
        }

        public String randomString(int length){
            if(length<=0)
                length=10;
            String str="qwertyuiopasdfghjklzxcvbnm";
            StringBuilder builder=new StringBuilder();
            Random random=new Random(System.currentTimeMillis()+number*4);
            for(int i=0;i<length;i++){
                builder.append(str.charAt(random.nextInt(str.length())));
            }
            return builder.toString();
        }

        private void crawlCookie(){
            try{
                System.setProperty("webdriver.chrome.driver",PropertiesUtil.getValue("chromedriver"));
                ChromeOptions options=new ChromeOptions();
                if(Boolean.valueOf(PropertiesUtil.getValue("proxy.use"))){
                    HttpHost httpHost=HttpClientUtil.getHttpProxy(number+11);
                    String proxyStr=httpHost.getHostName()+":"+httpHost.getPort();
                    Map<String, Object> map = new HashMap<>();
                    map.put("httpProxy", proxyStr);
                    options.setProxy(new Proxy(map));
                }
                options.addArguments("--incognito");
                options.addArguments("--headless");
                options.addArguments("--no-sandbox");
                String ua=randomString(10);
                options.addArguments("user-agent="+ua);
                WebDriver webDriver=new ChromeDriver(options);
                webDriver.get("https://gny.ly.com/list?src=北京&dest=云南&prop=1");
                Set<Cookie> cookies=webDriver.manage().getCookies();
                Iterator<Cookie> iterator=cookies.iterator();
                while((iterator.hasNext())){
                    Cookie cookie=iterator.next();
                    if(cookie.getName().contains("dj-po")){
                        String cookieStr=cookie.getName()+"="+cookie.getValue()+";";
                        Core.cookies[number]=cookieStr;
                    }
                }
                webDriver.close();
            }catch(Exception e){
                System.out.println("failed to crawl cookies");
                e.printStackTrace();
                System.exit(-1);
            }
        }
        @Override
        public void run(){
            while(true){
                crawlCookie();
                ThreadUtil.waitSecond(60*2);
            }
        }
    }

    public static void main(String[] args){
        CookiePool cookiePool=new CookiePool(5);
        cookiePool.start();
    }
}

