package com.alibaba.dubbo.performance.demo.agent.cache;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 王天华 on 2018/5/21.
 */
public class CacheManager {

    private static ConcurrentHashMap<Endpoint, Integer> cacheMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<Endpoint, Integer> getInstance(){
        if(null == cacheMap){
            synchronized(CacheManager.class){
                if(null == cacheMap){
                    return new ConcurrentHashMap<>();
                }
            }
        }
        return cacheMap;
    }
    /**
     * 获取缓存的对象
     *
     * @param endpoint
     * @return
     */
    public static Integer getCache(Endpoint endpoint) {

        return cacheMap.get(endpoint);
    }

    /**
     * 初始化缓存
     *
     * @param endpoints
     */
     public static void initCache(List<Endpoint> endpoints) {
         endpoints.forEach(endpoint -> cacheMap.put(endpoint, Integer.valueOf(endpoint.getBalanceWeight())));

    }

    public static void putCache(Endpoint endpoint, Integer currentWeight){
         cacheMap.put(endpoint, currentWeight);
    }

}
