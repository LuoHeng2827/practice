package com.luoheng.example.xiecheng;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class HrefCrawlerFactory extends BasicCrawlerFactory<HrefCrawler>{
    public HrefCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public HrefCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        return false;
    }

    @Override
    public HrefCrawler newInstance(){
        HrefCrawler crawler=new HrefCrawler(this);
        return crawler;
    }

    @Override
    public Vector<HrefCrawler> newVector(int count){
        Vector<HrefCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            HrefCrawler crawler=newInstance();
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
