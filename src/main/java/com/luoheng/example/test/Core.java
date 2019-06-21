package com.luoheng.example.test;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ThreadUtil;

import java.util.HashMap;
import java.util.Map;

public class Core{
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        CrawlerFactory factory1=new Crawler1Factory(controller);
        CrawlerFactory factory2=new Crawler2Factory(controller);
        Map<String,Integer> rq1=new HashMap<>();
        rq1.put(Crawler1.TO_QUEUE,10);
        controller.add(factory1,2,rq1).add(factory2,1);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitMillis(1000);
        }
    }
}
