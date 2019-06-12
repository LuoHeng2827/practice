package com.luoheng.example.lcrawler;


import com.luoheng.example.util.ThreadUtil;


public abstract class Crawler extends Thread{
    private static final long DEFAULT_CRAWL_INTERVAL=1000L;
    private CrawlerController controller;
    private boolean over;
    private long crawlInterval;

    public Crawler(CrawlerController controller){
        this.controller=controller;
        init();
    }

    public Crawler(CrawlerController controller,String name){
        this(controller);
        setName(name);
    }

    public Crawler(CrawlerController controller,String name,long crawlInterval){
        this(controller, name);
        this.crawlInterval=crawlInterval;
    }

    public void init(){
        over=false;
        crawlInterval=DEFAULT_CRAWL_INTERVAL;
    }

    public abstract String getTaskData();

    public abstract void crawl(String taskData);

    public boolean isOver() {
        return over;
    }

    public void over() {
        over=true;
    }

    public void setCrawlInterval(long crawlInterval) {
        if(crawlInterval>0)
            this.crawlInterval = crawlInterval;
    }

    @Override
    public void run() {
        while(!over){
            long startTime=System.currentTimeMillis();
            crawl(getTaskData());
            long endTime=System.currentTimeMillis();
            if(endTime-startTime<crawlInterval)
                ThreadUtil.waitMillis(crawlInterval-endTime+startTime);
        }
    }
}
