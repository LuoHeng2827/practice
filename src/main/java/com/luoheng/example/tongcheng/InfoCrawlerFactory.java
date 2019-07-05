package com.luoheng.example.tongcheng;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.Vector;

public class InfoCrawlerFactory extends BasicCrawlerFactory<InfoCrawler>{
    public InfoCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public InfoCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        long len=JedisUtil.llen(InfoCrawler.FROM_QUEUE);
        return len<=0;
    }

    @Override
    public InfoCrawler newInstance(){
        return new InfoCrawler(this);
    }

    @Override
    public Vector<InfoCrawler> newVector(int count){
        Vector<InfoCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            InfoCrawler crawler=newInstance();
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
