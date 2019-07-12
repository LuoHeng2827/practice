package com.luoheng.example._tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class InfoCrawlerFactory extends BasicCrawlerFactory<InfoCrawler> {
    private CrawlerController controller;
    private int type;
    public InfoCrawlerFactory(CrawlerController controller){
        super(controller);
        this.controller=controller;
        this.type=type;
    }
    @Override
    public InfoCrawler newInstance() {
        return new InfoCrawler(this);
    }

    @Override
    public Vector<InfoCrawler> newVector(int count) {
        Vector<InfoCrawler> crawlerVector=new Vector<>(count);
        for(int i=0;i<count;i++){
            InfoCrawler crawler=new InfoCrawler(this);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }

    @Override
    public boolean shouldOver(){
        return false;
    }
}
