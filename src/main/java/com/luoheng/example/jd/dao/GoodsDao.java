package com.luoheng.example.jd.dao;

import com.luoheng.example.Dao;
import com.luoheng.example.DaoImp;
import com.luoheng.example.jd.bean.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("unchecked")
@Repository
public class GoodsDao extends DaoImp<Goods> {
}
