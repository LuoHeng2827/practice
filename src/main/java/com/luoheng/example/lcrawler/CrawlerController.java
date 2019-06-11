package com.luoheng.example.lcrawler;

import com.luoheng.example.util.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class CrawlerController implements Runnable{
    private static final long DEFAULT_MONITOR_INTERVAL=1000*5;
    private long monitorInterval;
    private boolean complete;
    private Map<CrawlerFactory,Vector<Crawler>> allCrawler=new HashMap<>();

    public CrawlerController(){
        defaultInit();
    }

    private void defaultInit(){
        monitorInterval=DEFAULT_MONITOR_INTERVAL;
    }

    public CrawlerController add(CrawlerFactory factory,int count){
        Vector<Crawler> crawlerVector=factory.newVector(count);
        allCrawler.put(factory,crawlerVector);
        return this;
    }

    private void monitorCrawler(){
        complete=true;
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            Vector<Crawler> crawlerVector=entry.getValue();
            CrawlerFactory factory=entry.getKey();
            for(Crawler crawler:crawlerVector){
                if(!crawler.isAlive()){
                    if(!crawler.isOver()){
                        crawlerVector.remove(crawler);
                        Crawler newCrawler=factory.newInstance();
                        newCrawler.start();
                        crawlerVector.add(newCrawler);
                        complete=false;
                    }
                }
                else{
                    complete=false;
                }
            }
        }
    }

    private void startCrawler(){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            Vector<Crawler> crawlerVector=entry.getValue();
            for(Crawler crawler:crawlerVector){
                crawler.start();
            }
        }
    }

    @Override
    public void run() {
        startCrawler();
        while(true){
            monitorCrawler();
            if(complete)
                break;
            ThreadUtil.waitMillis(monitorInterval);
        }
    }
}
