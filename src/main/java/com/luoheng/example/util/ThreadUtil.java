package com.luoheng.example.util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {
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
        LogUtil.i(uuid);
        LogUtil.i(uuid.length()+"");
    }
}
