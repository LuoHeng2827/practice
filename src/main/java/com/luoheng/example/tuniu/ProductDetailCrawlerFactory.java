package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class ProductDetailCrawlerFactory extends BasicCrawlerFactory<ProductDetailCrawler> {
    private CrawlerController controller;
    private int type;
    public ProductDetailCrawlerFactory(CrawlerController controller,int type){
        super(controller);
        this.controller=controller;
        this.type=type;
    }
    @Override
    public ProductDetailCrawler newInstance() {
        return new ProductDetailCrawler(this,type);
    }

    @Override
    public Vector<ProductDetailCrawler> newVector(int count) {
        Vector<ProductDetailCrawler> crawlerVector=new Vector<>(count);
        for(int i=0;i<count;i++){
            ProductDetailCrawler crawler=new ProductDetailCrawler(this,type);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
