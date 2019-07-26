package com.luoheng.example.test;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ThreadUtil;

import java.util.HashMap;
import java.util.Map;

public class Core{
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        Crawler1Factory factory1=new Crawler1Factory(controller);
        Crawler2Factory factory2=new Crawler2Factory(controller);
        controller.add(factory1,10).add(factory2,1);
        controller.start();
        while(!controller.isComplete()){
            controller.printStatus();
            ThreadUtil.waitMillis(1000);
        }
    }
}
