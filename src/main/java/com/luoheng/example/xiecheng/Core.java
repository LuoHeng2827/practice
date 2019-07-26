package com.luoheng.example.xiecheng;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;

public class Core{
    public static final String ERROR_QUEUE="xiecheng_error_msg";
    public static boolean isUpdatePrice=false;
    public static void main(String[] args){
        CrawlerController controller=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactory=new ProductListCrawlerFactory(controller);
        HrefCrawlerFactory hrefCrawlerFactory=new HrefCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(productListCrawlerFactory,Integer.parseInt(args[0]))
                .add(hrefCrawlerFactory,Integer.parseInt(args[1]))
                .add(infoCrawlerFactory,Integer.parseInt(args[2]))
                .add(dbTaskCrawlerFactory,Integer.parseInt(args[3]));
        isUpdatePrice=Boolean.parseBoolean(args[4]);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitSecond(1);
        }
    }
    public static void saveErrorMsg(String task){
        List<String> failedTasks=JedisUtil.lrange(ERROR_QUEUE,0,JedisUtil.llen(ERROR_QUEUE));
        for(String failedTask:failedTasks){
            if(failedTask.equals(task))
                return;
        }
        JedisUtil.lpush(ERROR_QUEUE,task);
    }
}
