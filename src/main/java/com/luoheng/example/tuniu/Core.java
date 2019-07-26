package com.luoheng.example.tuniu;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;

public class Core{
    public static String ERROR_QUEUE="tuniu_error_msg";
    public static boolean isUpdatePrice=false;
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactoy=new ProductListCrawlerFactory(controller);
        AllHrefCrawlerFactory allHrefCrawlerFactory=new AllHrefCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(productListCrawlerFactoy,Integer.parseInt(args[0]))
                .add(allHrefCrawlerFactory,Integer.parseInt(args[1]))
                .add(infoCrawlerFactory,Integer.parseInt(args[2]))
                .add(dbTaskCrawlerFactory,Integer.parseInt(args[3]));
        isUpdatePrice=Boolean.valueOf(args[4]);
        controller.start();
        if(!controller.isComplete()){
            ThreadUtil.waitSecond(3);
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
