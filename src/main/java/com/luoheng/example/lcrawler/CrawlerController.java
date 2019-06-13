package com.luoheng.example.lcrawler;

import com.luoheng.example.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 *
 */
public class CrawlerController extends Thread{
    private static final long DEFAULT_MONITOR_INTERVAL=1000*5;
    private long monitorInterval;
    private boolean complete;
    private Map<CrawlerFactory,Vector<Crawler>> allCrawler=new LinkedHashMap<>();
    private Logger logger=LogManager.getLogger(CrawlerController.class);

    public CrawlerController(){
        defaultInit();
    }

    public CrawlerController(String name){
        this();
        setName(name);
    }

    private void defaultInit(){
        monitorInterval=DEFAULT_MONITOR_INTERVAL;
    }

    /**
     * 添加节点，形成爬虫工作链
     * @param factory 管理爬虫生命周期的工厂类
     * @param count 要创建爬虫的数量
     * @return 返回自身，用于链式调用
     */
    public CrawlerController add(CrawlerFactory factory,int count){
        Vector<Crawler> crawlerVector=factory.newVector(count);
        allCrawler.put(factory,crawlerVector);
        return this;
    }

    /**
     * 通知给定爬虫工厂的下一个爬虫工厂关闭
     * @param factory
     */
    public void notifyNextOver(CrawlerFactory factory){
        boolean isNext=false;
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            if(isNext){
                entry.getKey().notifyOver();
                break;
            }
            if(entry.getKey().equals(factory)){
                isNext=true;
            }
        }
    }

    /**
     * 监视爬虫工厂是否关闭，如果关闭，则通知下一个爬虫工厂关闭
     */
    private void monitorCrawlerFactory(){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            entry.getKey().inspect();
            if(entry.getKey().isOver())
                notifyNextOver(entry.getKey());
        }
    }

    /**
     * 监视爬虫数量，若线程因为不可逆性的原因关闭，则重新创建爬虫不足数量
     */
    private void monitorCrawlerCount(){
        complete=true;
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            Vector<Crawler> crawlerVector=entry.getValue();
            CrawlerFactory factory=entry.getKey();
            if(factory.isOver())
                continue;
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

    /**
     * 获得爬虫工厂需要管理的爬虫数组
     * @param factory
     * @return
     */
    public Vector<? extends Crawler> getFactoryVector(CrawlerFactory factory){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry:allCrawler.entrySet()){
            if(entry.getKey().equals(factory))
                return entry.getValue();
        }
        return null;
    }


    public void monitor(){
        monitorCrawlerFactory();
        monitorCrawlerCount();
    }

    /**
     * 启动爬虫线程
     */
    public void startCrawler(){
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
            monitor();
            if(complete)
                break;
            ThreadUtil.waitMillis(monitorInterval);
        }
        logger.info(this.getName()+" has finished");
    }

    public boolean isComplete() {
        return complete;
    }
}
