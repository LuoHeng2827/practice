package com.luoheng.example._tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class ProductListCrawlerFactory extends BasicCrawlerFactory<ProductListCrawler> {
    private CrawlerController controller;
    public ProductListCrawlerFactory(CrawlerController controller){
        super(controller);
        this.controller=controller;
    }
    @Override
    public ProductListCrawler newInstance() {
        return new ProductListCrawler(this);
    }

    @Override
    public Vector<ProductListCrawler> newVector(int count) {
        Vector<ProductListCrawler> crawlerVector=new Vector<>(count);
        for(int i=0;i<count;i++){
            ProductListCrawler crawler=new ProductListCrawler(this);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }

    @Override
    public boolean shouldOver(){
        return false;
    }
}
