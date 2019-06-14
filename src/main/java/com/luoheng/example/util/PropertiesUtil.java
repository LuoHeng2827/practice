package com.luoheng.example.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
    }
}
