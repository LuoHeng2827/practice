package com.luoheng.example.mafengwo;

import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.List;


public class Core{
    private static final String ERROR_QUEUE="mafengwo_error_msg";
    public static void main(String[] args) throws Exception{
        CrawlerController controller=new CrawlerController();
        PreparedCrawlerFactory preparedCrawlerFactory=new PreparedCrawlerFactory(controller);
        InfoCrawlerFactory infoCrawlerFactory=new InfoCrawlerFactory(controller);
        DBTaskCrawlerFactory dbTaskCrawlerFactory=new DBTaskCrawlerFactory(controller);
        controller.add(preparedCrawlerFactory,1)
                .add(infoCrawlerFactory,1)
                .add(dbTaskCrawlerFactory,1);
        controller.start();
        while(!controller.isComplete()){
            ThreadUtil.waitMillis(1000*3);
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
