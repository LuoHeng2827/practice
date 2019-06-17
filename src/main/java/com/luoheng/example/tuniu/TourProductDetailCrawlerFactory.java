package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class TourProductDetailCrawlerFactory extends BasicCrawlerFactory<TourProductDetailCrawler> {
    private CrawlerController controller;
    private int type;
    public TourProductDetailCrawlerFactory(CrawlerController controller){
        super(controller);
        this.controller=controller;
        this.type=type;
    }
    @Override
    public TourProductDetailCrawler newInstance() {
        return new TourProductDetailCrawler(this);
    }

    @Override
    public Vector<TourProductDetailCrawler> newVector(int count) {
        Vector<TourProductDetailCrawler> crawlerVector=new Vector<>(count);
        for(int i=0;i<count;i++){
            TourProductDetailCrawler crawler=new TourProductDetailCrawler(this);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
