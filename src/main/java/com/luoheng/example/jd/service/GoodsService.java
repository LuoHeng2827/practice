package com.luoheng.example.jd.service;

import com.luoheng.example.jd.bean.Goods;
import com.luoheng.example.jd.dao.GoodsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class GoodsService {
    GoodsDao goodsDao;
    public void saveGoods(Goods goods){
        goodsDao.save(goods);
    }
    @Autowired
    public void setGoodsDao(GoodsDao goodsDao) {
        this.goodsDao = goodsDao;
    }
}
