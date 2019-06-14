package com.luoheng.example.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {
    private Logger logger=LogManager.getLogger(ThreadUtil.class);
    public static void waitMillis(long millis){
        try{
            TimeUnit.MILLISECONDS.sleep(millis);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    public static void waitSecond(long second){
        try{
            TimeUnit.SECONDS.sleep(second);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    public static String getUUID(){
        String uuid=UUID.randomUUID().toString().replace("-","");
        return uuid;
    }
    public static void main(String[] args){
        String uuid=getUUID();
    }
}
