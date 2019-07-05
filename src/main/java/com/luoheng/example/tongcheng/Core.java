package com.luoheng.example.tongcheng;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;

public class Core{
    public static final String FAILED_QUEUE="tongcheng_failed_task";
    public static String cookie="dj-po=b445b98793a7a7d097f708578b3406d5;";
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactory=new ProductListCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(productListCrawlerFactory,1)
                .add(infoCrawlerFactory,10)
                .add(dbTaskCrawlerFactory,1);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitSecond(5);
        }
    }
    public static void saveErrorMsg(String task){
        List<String> failedTasks=JedisUtil.lrange(FAILED_QUEUE,0,JedisUtil.llen(FAILED_QUEUE));
        for(String failedTask:failedTasks){
            if(failedTask.equals(task))
                return;
        }
        JedisUtil.lpush(FAILED_QUEUE,task);
    }
}
