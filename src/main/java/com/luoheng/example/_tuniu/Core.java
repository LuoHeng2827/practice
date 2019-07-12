package com.luoheng.example._tuniu;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;

public class Core {
    private static final String ERROR_QUEUE="tuniu_error_msg";
    public static void main(String[] args){
        CrawlerController tourController=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactory1=new ProductListCrawlerFactory(tourController);
        InfoCrawlerFactory infoCrawlerFactory1=
                new InfoCrawlerFactory(tourController);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(tourController);
        tourController.add(productListCrawlerFactory1,1)
                .add(infoCrawlerFactory1,1)
                .add(dbTaskCrawlerFactory,1);
        tourController.start();
        while(true){
            if(tourController.isComplete())
                System.exit(0);
            ThreadUtil.waitMillis(3000);
        }
    }

    public static void saveErrorMsg(String errorMsg){
        long len=JedisUtil.llen(ERROR_QUEUE);
        List<String> msgs=JedisUtil.lrange(ERROR_QUEUE,0,len-1);
        for(String msg:msgs){
            if(msg.equals(errorMsg))
                return;
        }
        JedisUtil.lpush(ERROR_QUEUE,errorMsg);
    }
}
