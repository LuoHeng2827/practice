package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;

public class Core {
    public static void main(String[] args){
        CrawlerController tourController=new CrawlerController();
        //CrawlerController pkgController=new CrawlerController();
        ProductCrawlerFactory productCrawlerFactory1=new ProductCrawlerFactory(tourController,ProductCrawler.TYPE_TOUR);
        //ProductCrawlerFactory productCrawlerFactory2=new ProductCrawlerFactory(pkgController,ProductCrawler.TYPE_PKG);
        TourProductDetailCrawlerFactory tourProductDetailCrawlerFactory1=
                new TourProductDetailCrawlerFactory(tourController);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(tourController);
        tourController.add(productCrawlerFactory1,1)
                .add(tourProductDetailCrawlerFactory1,30)
                .add(dbTaskCrawlerFactory,30);
        //pkgController.add(productCrawlerFactory2,1).add(productDetailCrawlerFactory2,20);
        tourController.start();
        //pkgController.start();
        while(true){
            if(tourController.isComplete())
                System.exit(0);
            ThreadUtil.waitMillis(3000);
        }
    }
}
