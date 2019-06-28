package com.luoheng.example.test;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class Crawler2Factory extends BasicCrawlerFactory<Crawler2>{
    public Crawler2Factory(CrawlerController controller){
        super(controller);
    }

    public Crawler2Factory(CrawlerController controller, String name){
        super(controller, name);
    }

    @Override
    public Crawler2 newInstance(){
        return new Crawler2(this);
    }

    @Override
    public Vector<Crawler2> newVector(int count){
        Vector<Crawler2> crawler2Vector=new Vector<>();
        for(int i=0;i<count;i++){
            Crawler2 crawler=new Crawler2(this);
            crawler2Vector.add(crawler);
        }
        return crawler2Vector;
    }

    @Override
    public boolean shouldOver(){
        return false;
    }
}
