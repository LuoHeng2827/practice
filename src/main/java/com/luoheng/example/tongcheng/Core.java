package com.luoheng.example.tongcheng;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;
import java.util.Random;

public class Core{
    public static final String ERROR_QUEUE="tongcheng_error_msg";
    public static boolean isUpdatePrice=false;
    public static String[] cookies=new String[11];
    public static String UA="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36 OPR/37.0.2178.31";
    public static void main(String[] args) throws Exception{
        CookiePool cookiePool=new CookiePool(11);
        cookiePool.start();
        ThreadUtil.waitSecond(30);
        System.out.println("start!");
        CrawlerController controller=new CrawlerController();
        ProductListCrawlerFactory productListCrawlerFactory=new ProductListCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(productListCrawlerFactory,Integer.parseInt(args[0]))
                .add(infoCrawlerFactory,Integer.parseInt(args[1]))
                .add(dbTaskCrawlerFactory,Integer.parseInt(args[2]));
        isUpdatePrice=Boolean.parseBoolean(args[3]);
        long start=System.currentTimeMillis();
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitMillis(500);
        }
        long end=System.currentTimeMillis();
        System.out.println("complete! it use "+(end-start)/1000+"s");
        
    }
    public static void saveErrorMsg(String task){
        List<String> failedTasks=JedisUtil.lrange(ERROR_QUEUE,0,JedisUtil.llen(ERROR_QUEUE));
        for(String failedTask:failedTasks){
            if(failedTask.equals(task))
                return;
        }
        JedisUtil.lpush(ERROR_QUEUE,task);
    }

    public static String randomUA(int number,int length){
        if(length<=0)
            length=10;
        String str="qwertyuiopasdfghjklzxcvbnm";
        StringBuilder builder=new StringBuilder();
        Random random=new Random(System.currentTimeMillis()+number*4);
        for(int i=0;i<length;i++){
            builder.append(str.charAt(random.nextInt(str.length())));
        }
        return builder.toString();
    }
}
