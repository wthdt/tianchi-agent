package com.alibaba.dubbo.performance.demo.agent.loadBalance;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by 王天华 on 2018/5/22.
 */
public class WeightRandom<K,V extends Number> {
    private TreeMap<Double, K> weightMap = new TreeMap<>();

    public WeightRandom(List<Pair<K, V>> list) {
        Preconditions.checkNotNull(list, "list can NOT be null!");
        for (Pair<K, V> pair : list) {
            double lastWeight = this.weightMap.size() == 0 ? 0 : this.weightMap.lastKey();//统一转为double
            this.weightMap.put(pair.getValue().doubleValue() + lastWeight, pair.getKey());//权重累加
        }
    }

    public K random() {
        double randomWeight = this.weightMap.lastKey() * Math.random();
        SortedMap<Double, K> tailMap = this.weightMap.tailMap(randomWeight, false);
        return this.weightMap.get(tailMap.firstKey());
    }


    public static void main(String[] args){
        Endpoint endpoint1 = new Endpoint("127.0.0.1", 11, "1");
        Endpoint endpoint2 = new Endpoint("127.0.0.2", 11, "2");
        Endpoint endpoint3 = new Endpoint("127.0.0.3", 11, "3");
        Pair<Endpoint, Double> pair1 = new Pair<>(endpoint1, Double.valueOf(endpoint1.getBalanceWeight()));
        Pair<Endpoint, Double> pair2 = new Pair<>(endpoint2, Double.valueOf(endpoint2.getBalanceWeight()));
        Pair<Endpoint, Double> pair3 = new Pair<>(endpoint3, Double.valueOf(endpoint3.getBalanceWeight()));

        WeightRandom<Endpoint, Double> weightRandom = new WeightRandom(Lists.newArrayList(pair1,pair2,pair3));
        Endpoint endpointKey = weightRandom.random();
    }

}
