package com.luoheng.example.lvmama;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class ProductListCrawlerFactory extends BasicCrawlerFactory<ProductListCrawler>{
    public ProductListCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public ProductListCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        return ProductListCrawler.shouldOver;
    }

    @Override
    public ProductListCrawler newInstance(){
        return new ProductListCrawler(this);
    }

    @Override
    public Vector<ProductListCrawler> newVector(int count){
        Vector<ProductListCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            ProductListCrawler crawler=newInstance();
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
