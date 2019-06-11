package com.luoheng.example.jd;

import com.luoheng.example.util.LogUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringFactory {
    private static ApplicationContext applicationContext;
    static {
        init();
    }
    private static void init(){
        LogUtil.i("Spring factory is starting");
        applicationContext = new ClassPathXmlApplicationContext("springConfig.xml");
        LogUtil.i("Spring factory is started");
    }
    public Object getBean(String name){
        return applicationContext.getBean(name);
    }
}
