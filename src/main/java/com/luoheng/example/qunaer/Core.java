package com.luoheng.example.qunaer;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;

public class Core{
    public static final String ERROR_QUEUE="qunaer_error_msg";
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactory=new ProductListCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(productListCrawlerFactory,1)
                .add(infoCrawlerFactory,1)
                .add(dbTaskCrawlerFactory,1);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitSecond(3);
        }
    }

    public static void saveErrorMsg(String errorMsg){
        long len=JedisUtil.llen(ERROR_QUEUE);
        List<String> msgs=JedisUtil.lrange(ERROR_QUEUE,0,len);
        for(String msg:msgs){
            if(msg.contains(errorMsg))
                return;
        }
        JedisUtil.lpush(ERROR_QUEUE,errorMsg);
    }
}
