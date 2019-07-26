package com.luoheng.example.util.BloomFilter;


import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.redis.JedisUtil;

import java.util.ArrayList;
import java.util.List;

public class BFUtil {

    private static int MAX_SIZE;
    private static int HASH_TIMES;
    private static String bfName;

    static{
        String ip=PropertiesUtil.getValue("redis.ip");
        int port=Integer.parseInt(PropertiesUtil.getValue("redis.port"));
        String passwords=PropertiesUtil.getValue("redis.passwords");
        int dataSize=Integer.parseInt(PropertiesUtil.getValue("bf.dataSize"));
        double negativeRate=Double.parseDouble(PropertiesUtil.getValue("bf.negativeRate"));
        int hashCount=Integer.parseInt(PropertiesUtil.getValue("bf.hashCount"));
        BfConfiguration conf=new BfConfiguration(ip,port,passwords,hashCount,negativeRate,dataSize);
        MAX_SIZE=conf.getBitLength();
        HASH_TIMES=conf.getHashCount();
        bfName=PropertiesUtil.getValue("bf.bfName");
        JedisUtil.setbit(bfName,MAX_SIZE,false);
    }

    /**
     * 
     * @param str
     * @return false 存在  true 不存在，并添加
     */
    public static boolean add(String str){
        List<Integer> hashs = toHashs(str);
        if (isExist(hashs)){
            //判断在集合中是否存在
            return false;
        }else {
            //如果不存在，将对应的hash位置为1
            for (int hash:
                 hashs) {
                JedisUtil.setbit(bfName,hash,true);
            }
            return true;
        }
    }

    public static boolean isExist(List<Integer> hashs){
        boolean flag = true;
        for (int i = 0;(i < HASH_TIMES) && (flag)  ; i++) {
            flag = flag && JedisUtil.getbit(bfName,hashs.get(i));
        }
        return flag;
    }
    public static boolean isExist(String str){
    	 List<Integer> hashs = toHashs(str);
        boolean flag = true;
        for (int i = 0;(i < HASH_TIMES) && (flag)  ; i++) {
            flag = flag && JedisUtil.getbit(bfName,hashs.get(i));
        }
        return flag;
    }

    public static List<Integer> toHashs(String url){
        List<Integer> list = new ArrayList<Integer>();
        list.add(HashUtils.additiveHash(url, 47) % MAX_SIZE);
        list.add(HashUtils.rotatingHash(url, 47) % MAX_SIZE);
        list.add(HashUtils.oneByOneHash(url) % MAX_SIZE);
        list.add(HashUtils.bernstein(url) % MAX_SIZE);
        list.add(HashUtils.FNVHash(url.getBytes()) % MAX_SIZE);
        list.add(HashUtils.RSHash(url) % MAX_SIZE);
        list.add(HashUtils.JSHash(url) % MAX_SIZE);
        list.add(HashUtils.PJWHash(url) % MAX_SIZE);
        list.add(HashUtils.ELFHash(url) % MAX_SIZE);
        list.add(HashUtils.BKDRHash(url) % MAX_SIZE);
        list.add(HashUtils.SDBMHash(url) % MAX_SIZE);
        list.add(HashUtils.DJBHash(url) % MAX_SIZE);
        list.add(HashUtils.DEKHash(url) % MAX_SIZE);
        list.add(HashUtils.APHash(url) % MAX_SIZE);
        list.add(HashUtils.java(url) % MAX_SIZE);
        for (int i = 0; i < list.size(); i++) {
            list.set(i,Math.abs(list.get(i)));
        }
        return list;
    }
    public static void main(String[] args){

    }
}
