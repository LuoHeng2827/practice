package com.luoheng.example.lcrawler;

import java.util.Vector;

/**
 * 负责爬虫生命周期的工厂
 * @param <T>
 */
public interface CrawlerFactory<T extends Crawler> {
    /**
     * 生成爬虫实例
     * @return 返回爬虫实例
     */
    T newInstance();

    /**
     * 创建爬虫数组
     * @param count 爬虫数量
     * @return 返回爬虫数组
     */
    Vector<T> newVector(int count);

    /**
     * 通知工厂关闭爬虫
     */
    void notifyOver();

    /**
     * 判断工厂是否关闭
     * @return
     */
    boolean isStop();

    /**
     * 判断工厂是否结束
     * @return
     */
    boolean isOver();

    /**
     * 检查爬虫状态并进行相应的操作
     */
    void inspect();


}
