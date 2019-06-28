package com.luoheng.example.lcrawler;

import com.luoheng.example.util.redis.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;

/**
 *
 */
public class CrawlerController extends Thread{
    /**
     * 默认的监视函数执行间隔
     */
    private static final long DEFAULT_MONITOR_INTERVAL=100;
    /**
     * 默认的遗失任务的保存路径
     */
    private static final String DEFAULT_SAVE_LOST_TASK_PATH="./lost_task.txt";
    /**
     * 监视函数执行间隔
     */
    private long monitorInterval;
    /**
     * 判断爬取工作是否完成
     */
    private boolean complete;
    /**
     * 用来判断是否保存遗失任务的标识符，默认不保存
     */
    private boolean saveLostTask;
    /**
     * 遗失任务的保存路径
     */
    private String lostTaskPath;
    /**
     * 日志存储路径
     */
    private String logFilePath;
    /**
     * 用来保存爬虫工作链，Key为爬虫工厂，Value为爬虫数组
     */
    private double threshold=0.8D;
    private Map<CrawlerFactory,Vector<Crawler>> allCrawlers =new LinkedHashMap<>();
    private Map<CrawlerFactory,Map<String,Integer>> relatedRedisKey=new LinkedHashMap<>();
    private Logger logger=LogManager.getLogger(CrawlerController.class);

    public CrawlerController(){
        defaultInit();
    }

    public CrawlerController(String name){
        this();
        setName(name);
    }

    public CrawlerController saveLostTask(boolean saveLostTask){
        this.saveLostTask=saveLostTask;
        return this;
    }

    public CrawlerController monitorInterval(long monitorInterval){
        this.monitorInterval=monitorInterval;
        return this;
    }

    public CrawlerController lostTaskPath(String lostTaskPath){
        this.lostTaskPath=lostTaskPath;
        return this;
    }

    private void defaultInit(){
        monitorInterval=DEFAULT_MONITOR_INTERVAL;
        lostTaskPath=DEFAULT_SAVE_LOST_TASK_PATH;
        complete=false;
        saveLostTask=false;
    }

    /**
     * 添加节点，形成爬虫工作链
     * @param factory 管理爬虫生命周期的工厂类
     * @param count 要创建爬虫的数量
     * @return 返回自身，用于链式调用
     */
    @SuppressWarnings("unchecked")
    public CrawlerController add(CrawlerFactory factory,int count){
        Vector<Crawler> crawlerVector=factory.newVector(count);
        allCrawlers.put(factory,crawlerVector);
        return this;
    }

    public CrawlerController add(CrawlerFactory factory,int count,Map<String,Integer> redisKey){
        add(factory, count);
        relatedRedisKey.put(factory,redisKey);
        return this;
    }

    /**
     * 通知给定爬虫工厂的下一个爬虫工厂关闭
     * @param factory
     */
    public void notifyNextOver(CrawlerFactory factory){
        boolean isNext=false;
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry: allCrawlers.entrySet()){
            if(isNext){
                entry.getKey().notifyOver();
                break;
            }
            if(entry.getKey().equals(factory)){
                isNext=true;
            }
        }
    }

    private void monitorRedisKey(){
        for(Map.Entry<CrawlerFactory,Map<String,Integer>> entryI:relatedRedisKey.entrySet()){
            CrawlerFactory factory=entryI.getKey();
            Map<String,Integer> redisKeyMap=entryI.getValue();
            for(Map.Entry<String,Integer> entryJ:redisKeyMap.entrySet()){
                String redisKey=entryJ.getKey();
                int maxCount=entryJ.getValue();
                if(JedisUtil.llen(redisKey)>=maxCount){
                    if(!factory.isPause())
                        factory.pause();
                }
                else if(JedisUtil.llen(redisKey)<=maxCount*threshold){
                    if(factory.isPause())
                        factory.resume();
                }
            }
        }
    }

    /**
     * 监视爬虫工厂是否关闭，如果关闭，则通知下一个爬虫工厂关闭
     */
    private void monitorCrawlerFactory(){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry: allCrawlers.entrySet()){
            entry.getKey().inspect();
            if(entry.getKey().isOver())
                notifyNextOver(entry.getKey());
        }
    }

    /**
     * 监视爬虫数量，若线程因为不可逆性的原因关闭，则重新创建爬虫不足数量
     */
    private void monitorCrawlerCount(){
        complete=true;
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry: allCrawlers.entrySet()){
            Vector<Crawler> crawlerVector=entry.getValue();
            CrawlerFactory factory=entry.getKey();
            if(factory.isOver())
                continue;
            Iterator<Crawler> iterator=crawlerVector.iterator();
            Vector<Crawler> newCrawlerVector=new Vector<>();
            int index=0;
            while(iterator.hasNext()){
                Crawler crawler=iterator.next();
                if(!crawler.isAlive()){
                    if(!crawler.isOver()){
                        if(saveLostTask){
                            try{
                                saveLostTask(crawler.getCurrentTask());
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                        iterator.remove();
                        Crawler newCrawler=factory.newInstance();
                        newCrawler.setNumber(index);
                        newCrawlerVector.add(newCrawler);
                        newCrawler.start();
                        complete=false;
                    }
                }
                else{
                    complete=false;
                }
                index++;
            }
            crawlerVector.addAll(newCrawlerVector);
        }
    }

    private void saveLostTask(String lostTask) throws IOException {
        File file=new File(lostTaskPath);
        if(!file.exists())
            file.createNewFile();
        BufferedWriter fileWriter=new BufferedWriter(new FileWriter(file));
        fileWriter.write(lostTask+"\n");
        fileWriter.close();
    }

    /**
     * 获得爬虫工厂需要管理的爬虫数组
     * @param factory
     * @return
     */
    public Vector<? extends Crawler> getFactoryVector(CrawlerFactory factory){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry: allCrawlers.entrySet()){
            if(entry.getKey().equals(factory))
                return entry.getValue();
        }
        return null;
    }


    public void monitor(){
        monitorCrawlerFactory();
        monitorCrawlerCount();
        monitorRedisKey();
    }

    /**
     * 启动爬虫线程
     */
    public void startCrawler(){
        for(Map.Entry<CrawlerFactory,Vector<Crawler>> entry: allCrawlers.entrySet()){
            Vector<Crawler> crawlerVector=entry.getValue();
            for(Crawler crawler:crawlerVector){
                crawler.start();
            }
        }
    }

    @Override
    public void run() {
        startCrawler();
        while(true){
            monitor();
            if(complete)
                break;
            ThreadUtil.waitMillis(monitorInterval);
        }
        logger.info(this.getName()+" has finished");
    }

    public boolean isComplete() {
        return complete;
    }
}
