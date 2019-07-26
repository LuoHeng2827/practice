package com.luoheng.example.test;

import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.redis.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import redis.clients.jedis.Jedis;

public class Crawler2 extends Crawler{
    public static final String FROM_QUEUE="1";
    public Crawler2(CrawlerFactory factory){
        super(factory);
    }

    public Crawler2(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public Crawler2(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    @Override
    public String getTaskData(){
        return "";
    }

    @Override
    public void crawl(String taskData){
        doSomeThings();
    }
    private void doSomeThings(){
        ThreadUtil.waitSecond(5);
    }
}
