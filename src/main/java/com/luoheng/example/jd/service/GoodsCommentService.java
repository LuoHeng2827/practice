package com.luoheng.example.jd.service;

import com.luoheng.example.jd.bean.GoodsComment;
import com.luoheng.example.jd.dao.GoodsCommentDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class GoodsCommentService {
    GoodsCommentDao goodsCommentDao;

    public void saveGoodsComment(GoodsComment goodsComment){
        goodsCommentDao.save(goodsComment);
    }

    @Autowired
    public void setGoodsCommentDao(GoodsCommentDao goodsCommentDao) {
        this.goodsCommentDao = goodsCommentDao;
    }
}
