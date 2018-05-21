package com.alibaba.dubbo.performance.demo.agent.loadBalance;

import com.alibaba.dubbo.performance.demo.agent.cache.CacheManager;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 王天华 on 2018/5/16.
 */
public class RoundRobinByWeightLoadBalance {
    /**
     * Created by caojun on 2018/2/20.
     * <p>
     * 基本概念：
     * weight: 配置文件中指定的该后端的权重，这个值是固定不变的。
     * effective_weight: 后端的有效权重，初始值为weight。
     * 在释放后端时，如果发现和后端的通信过程中发生了错误，就减小effective_weight。
     * 此后有新的请求过来时，在选取后端的过程中，再逐步增加effective_weight，最终又恢复到weight。
     * 之所以增加这个字段，是为了当后端发生错误时，降低其权重。
     * current_weight:
     * 后端目前的权重，一开始为0，之后会动态调整。那么是怎么个动态调整呢？
     * 每次选取后端时，会遍历集群中所有后端，对于每个后端，让它的current_weight增加它的effective_weight，
     * 同时累加所有后端的effective_weight，保存为total。
     * 如果该后端的current_weight是最大的，就选定这个后端，然后把它的current_weight减去total。
     * 如果该后端没有被选定，那么current_weight不用减小。
     * <p>
     * 算法逻辑：
     * 1. 对于每个请求，遍历集群中的所有可用后端，对于每个后端peer执行：
     * peer->current_weight += peer->effecitve_weight。
     * 同时累加所有peer的effective_weight，保存为total。
     * 2. 从集群中选出current_weight最大的peer，作为本次选定的后端。
     * 3. 对于本次选定的后端，执行：peer->current_weight -= total。
     */
    //约定的invoker和权重的键值对

    public RoundRobinByWeightLoadBalance() {

    }

    /**
     * 算法逻辑：
     * 1. 对于每个请求，遍历集群中的所有可用后端，对于每个后端peer执行：
     * peer->current_weight += peer->effecitve_weight。
     * 同时累加所有peer的effective_weight，保存为total。
     * 2. 从集群中选出current_weight最大的peer，作为本次选定的后端。
     * 3. 对于本次选定的后端，执行：peer->current_weight -= total。
     *
     * @Return ivoker
     */
    public Endpoint select() {

        Integer total = 0;
        Endpoint nodeOfMaxWeight = null;
        ConcurrentHashMap<Endpoint, Integer> invokersWeight = CacheManager.getInstance();
        Integer valueMax = Integer.MIN_VALUE;
        for(Map.Entry<Endpoint, Integer> entry: invokersWeight.entrySet()) {
            total += Integer.valueOf(entry.getKey().getBalanceWeight());
            Integer value  = entry.getValue() + Integer.valueOf(entry.getKey().getBalanceWeight());
            if(value > valueMax){
                nodeOfMaxWeight = entry.getKey();
                valueMax = value;
            }
            CacheManager.putCache(entry.getKey(), value);
        }

        int value = CacheManager.getCache(nodeOfMaxWeight);
        CacheManager.putCache(nodeOfMaxWeight, value-total);
        return nodeOfMaxWeight;
    }

}
