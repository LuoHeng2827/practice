package com.luoheng.example.test;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class Crawler1Factory extends BasicCrawlerFactory<Crawler1>{
    public Crawler1Factory(CrawlerController controller){
        super(controller);
    }

    public Crawler1Factory(CrawlerController controller, String name){
        super(controller, name);
    }

    @Override
    public Crawler1 newInstance(){
        return new Crawler1(this);
    }

    @Override
    public Vector<Crawler1> newVector(int count){
        Vector<Crawler1> crawler1Vector=new Vector<>();
        for(int i=0; i<count; i++){
            Crawler1 crawler=new Crawler1(this);
            crawler1Vector.add(crawler);
        }
        return crawler1Vector;
    }

    @Override
    public boolean shouldOver(){
        return false;
    }
}
