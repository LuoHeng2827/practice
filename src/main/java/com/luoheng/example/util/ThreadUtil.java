package com.luoheng.example.util;

import java.util.concurrent.TimeUnit;

public class ThreadUtil {
    public static void waitMillis(long millis){
        try{
            TimeUnit.MILLISECONDS.sleep(millis);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
