package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class PreparedCrawlerFactory extends BasicCrawlerFactory<PreparedCrawler>{
    public PreparedCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public PreparedCrawlerFactory(CrawlerController controller, String name){
        super(controller, name);
    }

    @Override
    public PreparedCrawler newInstance(){
        return new PreparedCrawler(this);
    }

    @Override
    public Vector<PreparedCrawler> newVector(int count){
        Vector<PreparedCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            PreparedCrawler crawler=new PreparedCrawler(this);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }

    @Override
    public boolean shouldOver(){
        return PreparedCrawler.shouldOver;
    }
}
