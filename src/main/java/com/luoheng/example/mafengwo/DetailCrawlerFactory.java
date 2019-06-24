package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DetailCrawlerFactory extends BasicCrawlerFactory<DetailCrawler>{
    private List<Proxy> proxyList=new ArrayList<>();
    public DetailCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public DetailCrawlerFactory(CrawlerController controller, String name){
        super(controller, name);
    }


    @Override
    public DetailCrawler newInstance(){
        return new DetailCrawler(this);
    }

    @Override
    public Vector<DetailCrawler> newVector(int count){
        Vector<DetailCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            DetailCrawler crawler=new DetailCrawler(this);
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }

}
