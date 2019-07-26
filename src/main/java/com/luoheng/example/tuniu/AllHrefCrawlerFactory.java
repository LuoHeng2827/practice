package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.Vector;

public class AllHrefCrawlerFactory extends BasicCrawlerFactory<AllHrefCrawler>{
    public AllHrefCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public AllHrefCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        return JedisUtil.llen(AllHrefCrawler.FROM_QUEUE)<=0;
    }

    @Override
    public AllHrefCrawler newInstance(){
        return new AllHrefCrawler(this);
    }

    @Override
    public Vector<AllHrefCrawler> newVector(int count){
        Vector<AllHrefCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            AllHrefCrawler crawler=newInstance();
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
