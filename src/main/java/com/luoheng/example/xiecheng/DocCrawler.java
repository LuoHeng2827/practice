package com.luoheng.example.xiecheng;

import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;

public class DocCrawler extends Crawler{
    public static final String FROME_QUEUE="list_xiecheng_id";
    public static final String TO_QUEUE="";
    public DocCrawler(CrawlerFactory factory){
        super(factory);
    }

    public DocCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public DocCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        return null;
    }

    @Override
    public void crawl(String taskData){

    }
}
