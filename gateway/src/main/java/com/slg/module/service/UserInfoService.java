//package com.slg.module.service;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import com.slg.module.Dao.UserInfoDao;
//import com.slg.module.entity.UserInfo;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
//
//@Service
//public class UserInfoService {
//    @Autowired
//    private UserInfoDao dao;
//    Cache<Object, Object> cache = Caffeine.newBuilder()
//            //初始数量
//            .initialCapacity(10)
//            //最大条数
//            .maximumSize(10)
//            //expireAfterWrite和expireAfterAccess同时存在时，以expireAfterWrite为准
//            //最后一次写操作后经过指定时间过期
////            .expireAfterWrite(1, TimeUnit.SECONDS)
//            //最后一次读或写操作后经过指定时间过期
//            .expireAfterAccess(1, TimeUnit.SECONDS)
//            //监听缓存被移除
//            .removalListener((key, val, removalCause) -> {
//            })
//            //记录命中
//            .recordStats()
//            .build();
//
//    public UserInfo saveUser(UserInfo userInfo) {
//        return dao.save(userInfo);
//    }
//
////    public UserInfo getEntity(Function<Long, Integer> vFunc, Long gg) {
////        //vFunc.apply(999L) 999L 传给 s
////        Integer apply = vFunc.apply(999L);
////        return UserInfo;//10000+999L
////    }
////
////    public UserInfo getEntity(Function<Long, Integer> vFunc, Long gg) {
////
////    }
//
//
//}
