package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class ProductCrawlerFactory extends BasicCrawlerFactory<ProductCrawler> {
    private CrawlerController controller;
    private int type;
    public ProductCrawlerFactory(CrawlerController controller,int type){
        super(controller);
        this.controller=controller;
        this.type=type;
    }
    @Override
    public ProductCrawler newInstance() {
        return new ProductCrawler(this,type);
    }

    @Override
    public Vector<ProductCrawler> newVector(int count) {
        Vector<ProductCrawler> crawlerVector=new Vector<>(count);
        for(int i=0;i<count;i++){
            ProductCrawler crawler=new ProductCrawler(this,type);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
