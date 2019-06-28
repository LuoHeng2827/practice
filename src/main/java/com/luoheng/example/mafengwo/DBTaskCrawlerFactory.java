package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.Vector;

public class DBTaskCrawlerFactory extends BasicCrawlerFactory<DBTaskCrawler>{
    public DBTaskCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    @Override
    public DBTaskCrawler newInstance(){
        return new DBTaskCrawler(this);
    }

    @Override
    public Vector<DBTaskCrawler> newVector(int count){
        Vector<DBTaskCrawler> dbTaskCrawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            DBTaskCrawler crawler=new DBTaskCrawler(this);
            dbTaskCrawlerVector.add(crawler);
        }
        return dbTaskCrawlerVector;
    }

    @Override
    public boolean shouldOver(){
        long len=JedisUtil.llen(DBTaskCrawler.FROM_QUEUE);
        return len<=0;
    }
}
