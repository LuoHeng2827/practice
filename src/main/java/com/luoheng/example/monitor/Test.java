package com.luoheng.example.monitor;

import com.luoheng.example.util.LogUtil;
import com.luoheng.example.util.ThreadUtil;

public class Test {

    public static void doSomeThings() throws Exception{
        ThreadUtil.waitTime(1000);
        throw new Exception("this is exception");
    }

    public static void test1(){
        try{
            doSomeThings();
        }catch (Exception e){
            LogUtil.i("test1"+e.getMessage());
        }
    }

    public static void test2(){
        try{
            test1();
        }catch (Exception e){
            LogUtil.i("test2"+e.getMessage());

        }
    }

    public static void main(String[] args){
        Test.test2();
    }

}
