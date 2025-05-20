package com.slg.module.db.Dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SinglePrimaryKeyRepository<Obj, Key> implements JpaRepository<Object, Object> {

    Cache<Object, Object> cache = Caffeine.newBuilder()
            //初始数量
            .initialCapacity(10)
            //最大条数
            .maximumSize(10)
            //expireAfterWrite和expireAfterAccess同时存在时，以expireAfterWrite为准
            //最后一次写操作后经过指定时间过期
            .expireAfterWrite(1, TimeUnit.SECONDS)
            //最后一次读或写操作后经过指定时间过期
            .expireAfterAccess(1, TimeUnit.SECONDS)
            //监听缓存被移除
            .removalListener((key, val, removalCause) -> { })
            //记录命中
            .recordStats()
            .build();

    @Override
    public void flush() {

    }

    @Override
    public <S> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<Object> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Object> objects) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Object getOne(Object object) {
        return null;
    }

    @Override
    public Object getById(Object object) {
        return null;
    }

    @Override
    public Object getReferenceById(Object object) {
        return null;
    }

    @Override
    public <S> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S> S save(S entity) {
        return null;
    }

    @Override
    public <S> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<Object> findById(Object object) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Object object) {
        return false;
    }

    @Override
    public List<Object> findAll() {
        return null;
    }

    @Override
    public List<Object> findAllById(Iterable<Object> objects) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Object object) {

    }

    @Override
    public void delete(Object entity) {

    }

    @Override
    public void deleteAllById(Iterable<?> objects) {

    }

    @Override
    public void deleteAll(Iterable<?> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Object> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<Object> findAll(Pageable pageable) {
        return null;
    }
}
