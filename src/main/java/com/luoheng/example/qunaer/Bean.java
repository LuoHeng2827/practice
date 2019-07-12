package com.luoheng.example.qunaer;

import java.util.ArrayList;
import java.util.List;

public class Bean{
    //产品id
    String productId;
    //产品名字
    String productName;
    //产品链接
    String productLink;
    //旅行社名字
    String taName;
    //套餐信息
    Package bPackage=newPackage();
    //日历价钱
    List<Price> priceList=new ArrayList<>();
    //旅行时长
    String duration;

    public Price newPrice(){
        return new Price();
    }

    public Price newPrice(String date,float price){
        return new Price(date,price);
    }

    public Package newPackage(){
        return new Package();
    }


    public class Package{
        //套餐名字
        String name;
        //城市
        String cityName;
        //路线
        String path;

        @Override
        public String toString(){
            return "Package{"+
                    "name='"+name+'\''+
                    ", cityName='"+cityName+'\''+
                    ", path='"+path+'\''+
                    '}';
        }
    }
    public class Price{
        //日期，格式为yyyy-MM-dd
        String date;
        //价钱
        float price;

        public Price(){

        }

        public Price(String date,float price){
            this.date=date;
            this.price=price;
        }

        @Override
        public String toString(){
            return "Price{"+
                    "date='"+date+'\''+
                    ", price="+price+
                    '}';
        }
    }

    @Override
    public String toString(){
        return "Bean{"+
                "productId='"+productId+'\''+
                ", productName='"+productName+'\''+
                ", productLink='"+productLink+'\''+
                ", taName='"+taName+'\''+
                ", bPackage="+bPackage+
                ", priceList="+priceList+
                ", duration='"+duration+'\''+
                '}';
    }
}