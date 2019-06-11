package com.luoheng.example.jd.crawler;


import com.luoheng.example.jd.SpringFactory;
import org.springframework.stereotype.Component;

@Component
public class Core implements Runnable{
    private int goodsCrawlerCount;
    private int goodsCommentCrawlerCount;


    public Core(){
        this.goodsCrawlerCount=1;
        this.goodsCommentCrawlerCount=10;
    }

    public Core(int goodsCrawlerCount, int goodsCommentCrawlerCount){
        this.goodsCrawlerCount=goodsCrawlerCount;
        this.goodsCommentCrawlerCount=goodsCommentCrawlerCount;
    }
    @Override
    public void run() {
        SpringFactory factory=new SpringFactory();
        GoodsCrawler goodsCrawler=((GoodsCrawler)factory.getBean("goodsCrawler")).keyword("联合利华京东自营旗舰店");
        new Thread(goodsCrawler).start();
        for(int i=0;i<goodsCommentCrawlerCount;i++){
            GoodsCommentCrawler goodsCommentCrawler=(GoodsCommentCrawler)factory.getBean("goodsCommentCrawler");
            new Thread(goodsCommentCrawler).start();
        }
    }

    public static void main(String[] args){
        Core core=new Core(1,10);
        new Thread(core).start();
        /*SpringFactory factory=new SpringFactory();
        SessionFactory sessionFactory=(SessionFactory) factory.getBean("sessionFactory");
        GoodsService goodsService=(GoodsService)factory.getBean("goodsService");
        LogUtil.i((sessionFactory==null));
        Goods goods=new Goods();
        goods.setName("test");
        goodsService.saveGoods(goods);*/
    }
}
