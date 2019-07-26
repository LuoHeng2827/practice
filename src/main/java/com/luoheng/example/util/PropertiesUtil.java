package com.luoheng.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    private static Properties configProps=new Properties();
    static {
        try{
            InputStream is=PropertiesUtil.class.getResourceAsStream("/config.properties");
            configProps.load(is);
            is.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static String getValue(String key){
        return configProps.getProperty(key);
    }

    public static String getValue(String key,String defaultValue){
        return configProps.getProperty(key,defaultValue);
    }
    public static void main(String[] args){
    }
}
