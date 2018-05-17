package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.loadBalance.RoundRobinByWeightLoadBalance;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@RestController
public class HelloController {

    private Logger logger = LoggerFactory.getLogger(HelloController.class);
    
    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private static List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient = new OkHttpClient();

    private static Map<Endpoint, Integer> invokersWeight = new HashMap<>(3);

    @PostConstruct
    public void init(){
        endpoints.forEach(endpoint -> invokersWeight.put(endpoint, Integer.valueOf(endpoint.getBalanceWeight())));
    }

    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)){
            return consumer(interfaceName,method,parameterTypesString,parameter);
        }
        else if ("provider".equals(type)){
            return provider(interfaceName,method,parameterTypesString,parameter);
        }else {
            return "Environment variable type is needed to set to provider or consumer.";
        }
    }

    public byte[] provider(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {

        Object result = rpcClient.invoke(interfaceName,method,parameterTypesString,parameter);
        return (byte[]) result;
    }

    public Integer consumer(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {

        if (null == endpoints){
            synchronized (lock){
                if (null == endpoints){
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }


        // 简单的负载均衡，随机取一个
        Endpoint endpoint = selectProvider();

        String url =  "http://" + endpoint.getHost() + ":" + endpoint.getPort();

        RequestBody requestBody = new FormBody.Builder()
                .add("interface",interfaceName)
                .add("method",method)
                .add("parameterTypesString",parameterTypesString)
                .add("parameter",parameter)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            byte[] bytes = response.body().bytes();
            return JSON.parseObject(bytes, Integer.class);
        }
    }

    private Endpoint selectProvider(){
        RoundRobinByWeightLoadBalance roundRobin = new RoundRobinByWeightLoadBalance(invokersWeight);
        return roundRobin.select();

    }

    public static void main(String[] args){
        Integer times = 7;
        Endpoint endpoint1 = new Endpoint("127.0.0.1", 11, "1");
        Endpoint endpoint2 = new Endpoint("127.0.0.2", 11, "2");
        Endpoint endpoint3 = new Endpoint("127.0.0.3", 11, "3");
        invokersWeight.put(endpoint1, Integer.valueOf(endpoint1.getBalanceWeight()));
        invokersWeight.put(endpoint2, Integer.valueOf(endpoint2.getBalanceWeight()));
        invokersWeight.put(endpoint3, Integer.valueOf(endpoint3.getBalanceWeight()));

        RoundRobinByWeightLoadBalance roundRobin = new RoundRobinByWeightLoadBalance(invokersWeight);
        for (int i = 1; i <= times; i++) {
            System.out.print(new StringBuffer(i + "").append("    "));
            roundRobin.printCurrenctWeightBeforeSelect();
            Endpoint endpoint= roundRobin.select();
            System.out.print(new StringBuffer("    ").append(endpoint.getHost()).append("    "));
            roundRobin.printCurrenctWeight();
            System.out.println();
        }
    }
}
