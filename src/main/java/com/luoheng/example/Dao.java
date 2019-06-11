package com.luoheng.example;

import java.util.List;

public interface Dao<T> {
    void save(T t);

    void delete(T t);

    void update(T t);

    T findById(long id);

    List<T> findAll();
}
