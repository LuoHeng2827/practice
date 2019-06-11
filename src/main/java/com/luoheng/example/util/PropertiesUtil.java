package com.luoheng.example.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Properties;

@Component
public class PropertiesUtil {
    private static Properties configPros=new Properties();
    static {
        try{
            String path=PropertiesUtil.class.getClassLoader().getResource("").getPath();
            InputStream is=new BufferedInputStream(new FileInputStream(path+"/config.properties"));
            configPros.load(is);
            is.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static String getValue(String key){
        return configPros.getProperty(key);
    }

    public static String getValue(String key,String defaultValue){
        return configPros.getProperty(key,defaultValue);
    }
    public static void main(String[] args){
        LogUtil.i(getValue("redis_ip"));
        LogUtil.i(getValue("redis_passwords"));
    }
}
