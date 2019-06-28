package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;


public class Core{
    public static void main(String[] args) throws Exception{
        CrawlerController controller=new CrawlerController();
        PreparedCrawlerFactory preparedCrawlerFactory=new PreparedCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        PriceCrawlerFactory priceCrawlerFactory=new PriceCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(preparedCrawlerFactory,1)
                .add(infoCrawlerFactory,1)
                .add(priceCrawlerFactory,1)
                .add(dbTaskCrawlerFactory,1);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitMillis(1000*3);
        }
    }
}
