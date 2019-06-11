package com.luoheng.example.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class LogUtil {
    public static Logger logger= LogManager.getLogger(LogUtil.class);

    public static void i(String msg){
        logger.info(msg);
    }

    public static void i(boolean msg){
        logger.info(msg+"");
    }

    public static void e(String msg){
        logger.error(msg);
    }

    public static void w(String msg){
        logger.warn(msg);
    }

    public static void main(String[] args){
        LogUtil.i("hello");
    }

}
