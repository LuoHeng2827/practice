package com.luoheng.example.tongcheng;

import java.util.ArrayList;
import java.util.List;


public class Bean{
    public static final int SRC_MUSTER=0;
    public static final int DES_MUSTER=1;
    String productId;
    String productName;
    String productLink;
    String taName;
    Package bPackage=newPackage();
    List<Price> priceList=new ArrayList<>();
    int musterType;
    String musterPlace;

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
        String name;
        String cityName;
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
        String date;
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
                '}';
    }
}
