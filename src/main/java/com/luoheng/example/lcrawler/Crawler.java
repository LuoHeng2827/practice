package com.luoheng.example.lcrawler;


import com.luoheng.example.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Crawler extends Thread{
    private static final long DEFAULT_CRAWL_INTERVAL=100L;
    /**
     * 爬虫的当前任务
     */
    private String currentTask;
    /**
     * 爬虫结束标识符
     */
    private boolean over;
    /**
     * 爬取间隔
     */
    private long crawlInterval;
    /**
     * 负责该爬虫的工厂
     */
    private CrawlerFactory factory;
    private Logger logger=LogManager.getLogger(Crawler.class);

    public Crawler(CrawlerFactory factory){
        init();
        this.factory=factory;
    }

    public Crawler(CrawlerFactory factory,String name){
        this(factory);
        setName(name);
    }

    public Crawler(CrawlerFactory factory,String name,long crawlInterval){
        this(factory,name);
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
            currentTask=getTaskData();
            if(currentTask!=null)
                crawl(currentTask);
            long endTime=System.currentTimeMillis();
            if(endTime-startTime<crawlInterval)
                ThreadUtil.waitMillis(crawlInterval-endTime+startTime);
        }
        interrupt();
    }

    public CrawlerFactory getFactory() {
        return factory;
    }

    public String getCurrentTask() {
        return currentTask;
    }
}
