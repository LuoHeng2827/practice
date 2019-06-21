package com.luoheng.example.lcrawler;


import com.luoheng.example.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Crawler extends Thread{
    private static final long DEFAULT_CRAWL_INTERVAL=1000L;
    private static final long DEFAULT_PAUSE_SLEEP_TIME_UNIT=3000L;
    private static final long DEFAULT_MAX_PAUSE_TIME=20000L;
    private long pauseTimeUnit;
    private long pauseTime;
    /**
     * 爬虫的当前任务
     */
    private String currentTask;
    /**
     * 爬虫结束标识符
     */
    private boolean over;

    private boolean stop;
    /**
     * 爬取间隔
     */
    private long crawlInterval;
    /**
     * 负责该爬虫的工厂
     */
    private CrawlerFactory factory;
    private boolean pause;
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
        pause=false;
        crawlInterval=DEFAULT_CRAWL_INTERVAL;
        pauseTimeUnit=DEFAULT_PAUSE_SLEEP_TIME_UNIT;
    }

    public abstract String getTaskData();

    public abstract void crawl(String taskData);

    public boolean isOver() {
        return over;
    }

    public boolean isStop(){
        return stop;
    }

    public void stopThis(){
        stop=true;
    }

    public void overThis() {
        over=true;
    }

    public void setCrawlInterval(long crawlInterval) {
        if(crawlInterval>0)
            this.crawlInterval = crawlInterval;
    }

    public void pauseThis(){
        pause=true;
    }

    public void resumeThis(){
        pause=false;
        pauseTime=0;
    }

    public void setPauseTimeUnit(long pauseTimeUnit){
        this.pauseTimeUnit=pauseTimeUnit;
    }

    public boolean isPause() {
        return pause;
    }

    @Override
    public void run() {
        while(!over){
            if(pause){
                ThreadUtil.waitMillis(pauseTimeUnit);
                pauseTime+=pauseTimeUnit;
                if(pauseTime>=DEFAULT_MAX_PAUSE_TIME){
                    //todo 发送警告
                }
                continue;
            }
            long startTime=System.currentTimeMillis();
            currentTask=getTaskData();
            if(currentTask==null){
                if(isStop()){
                    overThis();
                    continue;
                }
            }
            else
                crawl(currentTask);
            long endTime=System.currentTimeMillis();
            if(endTime-startTime<crawlInterval)
                ThreadUtil.waitMillis(crawlInterval-endTime+startTime);
        }
    }

    public CrawlerFactory getFactory() {
        return factory;
    }

    public String getCurrentTask() {
        return currentTask;
    }
}
