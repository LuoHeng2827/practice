package com.luoheng.example.test;

import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.redis.JedisUtil;
import redis.clients.jedis.Jedis;

public class Crawler1 extends Crawler{
    public static final String TO_QUEUE="1";

    public Crawler1(CrawlerFactory factory){
        super(factory);
    }

    public Crawler1(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public Crawler1(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    @Override
    public String getTaskData(){
        return "taskData";
    }

    @Override
    public void crawl(String taskData){
        Jedis jedis=JedisUtil.getResource();
        jedis.lpush(TO_QUEUE,taskData);
    }

}
