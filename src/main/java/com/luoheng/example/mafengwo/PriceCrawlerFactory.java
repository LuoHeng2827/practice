package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.Vector;

public class PriceCrawlerFactory extends BasicCrawlerFactory<PriceCrawler>{
    public PriceCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public PriceCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public PriceCrawler newInstance(){
        return new PriceCrawler(this);
    }

    @Override
    public Vector<PriceCrawler> newVector(int count){
        Vector<PriceCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            PriceCrawler crawler=new PriceCrawler(this);
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }

    @Override
    public boolean shouldOver(){
        long len=JedisUtil.llen(PriceCrawler.FROM_QUEUE);
        return len<=0;
    }
}
