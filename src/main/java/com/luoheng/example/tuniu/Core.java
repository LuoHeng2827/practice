package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;

public class Core {
    public static void main(String[] args){
        CrawlerController tourController=new CrawlerController();
        CrawlerController pkgController=new CrawlerController();
        ProductCrawlerFactory productCrawlerFactory1=new ProductCrawlerFactory(tourController,ProductCrawler.TYPE_TOUR);
        ProductCrawlerFactory productCrawlerFactory2=new ProductCrawlerFactory(pkgController,ProductCrawler.TYPE_PKG);
        ProductDetailCrawlerFactory productDetailCrawlerFactory1=
                new ProductDetailCrawlerFactory(tourController,ProductDetailCrawler.TYPE_TOUR);
        ProductDetailCrawlerFactory productDetailCrawlerFactory2=
                new ProductDetailCrawlerFactory(pkgController,ProductDetailCrawler.TYPE_PKG);
        tourController.add(productCrawlerFactory1,1).add(productDetailCrawlerFactory1,20);
        pkgController.add(productCrawlerFactory2,1).add(productDetailCrawlerFactory2,20);
        tourController.start();
        pkgController.start();
        while(true){
            if(tourController.isComplete()&&pkgController.isComplete())
                System.exit(0);
            ThreadUtil.waitMillis(3000);
        }
    }
}
