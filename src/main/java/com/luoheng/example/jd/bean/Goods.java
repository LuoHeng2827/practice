package com.luoheng.example.jd.bean;

import javax.persistence.*;

@Entity
@Table(name = "t_goods")
public class Goods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    @Column(name="image_url",length = 256)
    private String imageUrl;
    @Column(name = "ware_id",length = 20)
    private String wareId;
    @Column(name = "name",length = 100)
    private String name;
    @Column(name = "price",length = 20)
    private String price;

    public void setId(long id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setWareId(String wareId) {
        this.wareId = wareId;
    }

    public long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getWareId() {
        return wareId;
    }

    @Override
    public String toString() {
        return "Goods{" +
                "imageUrl='" + imageUrl + '\'' +
                ", wareId='" + wareId + '\'' +
                ", name='" + name + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}
