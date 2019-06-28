package com.luoheng.example.xiecheng;

import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;

import java.util.Vector;

public class DocCrawlerFactory extends BasicCrawlerFactory<DocCrawler>{
    public DocCrawlerFactory(CrawlerController controller){
        super(controller);
    }

    public DocCrawlerFactory(CrawlerController controller,String name){
        super(controller,name);
    }

    @Override
    public boolean shouldOver(){
        return false;
    }

    @Override
    public DocCrawler newInstance(){
        return new DocCrawler(this);
    }

    @Override
    public Vector<DocCrawler> newVector(int count){
        Vector<DocCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            DocCrawler crawler=newInstance();
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
