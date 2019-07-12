package com.luoheng.example.qunaer;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.Vector;

public class DBTaskCrawlerFactory extends BasicCrawlerFactory<DBTaskCrawler>{
    public DBTaskCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public DBTaskCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        return JedisUtil.llen(DBTaskCrawler.FROM_QUEUE)<=0;
    }

    @Override
    public DBTaskCrawler newInstance(){
        return new DBTaskCrawler(this);
    }

    @Override
    public Vector<DBTaskCrawler> newVector(int count){
        Vector<DBTaskCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            DBTaskCrawler crawler=newInstance();
            crawler.setNumber(i);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
