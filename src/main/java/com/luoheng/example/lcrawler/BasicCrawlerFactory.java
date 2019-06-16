package com.luoheng.example.lcrawler;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

public abstract class BasicCrawlerFactory<T extends Crawler> implements CrawlerFactory<T> {
    private boolean stop;
    private boolean over;
    private boolean pause;
    private CrawlerController controller;
    private String name;
    private Logger logger=LogManager.getLogger(BasicCrawlerFactory.class);
    public BasicCrawlerFactory(CrawlerController controller){
        init();
        this.controller=controller;
    }
    public BasicCrawlerFactory(CrawlerController controller,String name){
        this(controller);
        this.name=name;
    }
    private void init(){
        stop=false;
        over=false;
        pause=false;
        name="CrawlerFactory";
    }

    /**
     * 通知工厂结束
     * 若工厂关闭或关闭
     */
    @Override
    @SuppressWarnings("unchecked")
    public void notifyOver(){
        if(isOver()||isStop())
            return;
        stop=true;
        logger.info(this.name+" is stopping...");
        Vector<T> crawlerVector=(Vector<T>)controller.getFactoryVector(this);
        for(T crawler:crawlerVector){
            crawler.over();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void pause(){
        pause=true;
        Vector<T> crawlerVector=(Vector<T>)controller.getFactoryVector(this);
        for(T crawler:crawlerVector){
            crawler.pauseThis();
        }
    }
    @Override
    public void resume(){
        pause=false;
    }

    /**
     * 检查状态，如果工厂已经结束，则返回。
     * 如果工厂没有关闭，通知爬虫线程关闭，如果爬虫线程全部关闭，令over为true
     */
    @Override
    @SuppressWarnings("unchecked")
    public void inspect() {
        if(isOver())
            return;
        if(isStop()){
            boolean over=true;
            Vector<T> crawlerVector=(Vector<T>)controller.getFactoryVector(this);
            for(T crawler:crawlerVector){
                crawler.over();
                if(crawler.isAlive()){
                    over=false;
                }
            }
            this.over=over;
            if(isOver())
                logger.info(this.name+" is over");
        }
    }

    @Override
    public boolean isStop() {
        return stop;
    }

    @Override
    public boolean isOver() {
        return over;
    }

    @Override
    public boolean isPause() {
        return pause;
    }

}
