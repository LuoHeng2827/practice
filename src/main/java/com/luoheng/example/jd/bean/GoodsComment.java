package com.luoheng.example.jd.bean;

import javax.persistence.*;

@Entity
@Table(name = "t_goods_comment")
public class GoodsComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    @Column(name = "content",columnDefinition = "longtext")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "GoodsComment{" +
                "content='" + content + '\'' +
                '}';
    }
}
