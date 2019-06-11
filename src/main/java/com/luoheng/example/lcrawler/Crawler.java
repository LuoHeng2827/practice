package com.luoheng.example.lcrawler;


import java.util.Vector;

public abstract class Crawler extends Thread{
    private CrawlerController controller;
    private boolean over;

    public Crawler(CrawlerController controller){
        this.controller=controller;
    }

    public Crawler(CrawlerController controller,String name){
        this(controller);
        setName(name);
    }

    public abstract String getTaskData();

    public abstract String crawl(String taskData);

    public abstract String handleData(String data);

    public abstract void saveProcessData(String processData);

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    @Override
    public void run() {
        String rawData=crawl(getTaskData());
        String processData=handleData(rawData);
        saveProcessData(processData);
    }


    public  abstract Vector<Crawler> createVector(int count);
}
