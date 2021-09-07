

**我的分类:**
 
Java相关 |Mysql|Elasticsearch
:---:|:---:|:-----------:
[☕](#java)|[💾](#mysql)|[🎨](#Elasticsearch)


## <span id="java">☕Java相关</span>
### 1.Java 8 Features
* Lambda表达式
* 接口的默认方法与静态方法
* 方法引用（含构造方法引用）
* 重复注解、扩展注解的支持（类型注解）
* Optional
* Stream
* Date/Time API (JSR 310)
* JavaScript引擎Nashorn
* Base64

### 2.线程池
为什么要使用线程池？直接new个线程不行吗？

如果我们在方法中直接 new 一个线程来处理，当这个方法被调用频繁时就会创建很多线程，不仅会消耗系统资源，还会降低系统的稳定性，一不小心把系统搞崩了。

如果我们合理的使用线程池，则可以避免把系统搞崩的窘境，总得来说，使用线程池可以带来以下几个好处：
* 降低资源消耗。通过重复利用已创建的线程，降低线程创建和销毁造成的消耗。
* 提高响应速度。当任务到达时，任务可以不需要等到线程创建就能立即执行。
* 增加线程的可管理型。线程是稀缺资源，使用线程池可以进行统一分配，调优和监控。

```.java
@Configuration
@EnableAsync
@SuppressWarnings("all")
public class AsyncConfiguration {
    // 声明一个线程池(并指定线程池的名字)
    @Bean("taskExecutor-monitor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数：线程池创建时候初始化的线程数
        executor.setCorePoolSize(8);
        //最大线程数：线程池最大的线程数。当核心线程都在忙，且缓冲队列都满了，才会申请超过核心线程数的线程
        executor.setMaxPoolSize(8);
        //缓冲队列：用来缓冲执行任务的队列。当核心线程都在忙，再来新的任务，会将任务放到缓冲队列
        executor.setQueueCapacity(5000);
        //允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(60);
        //线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("equipment-monitor-task-");
        executor.initialize();
        return executor;
    }
}

@Override
public void saveDeviceStatus() {
    DeviceIdIplist deviceIdIplist = getDeviceIdIplist();
    for (DeviceIdIplist.DeviceIdIp deviceIdIp : deviceIdIplist.getDeviceIdIpsList()) {
            deviceActionService.saveDeviceStatus(deviceIdIp);  
    }
}

//用当前ping的状态和redis中的状态比较
//如果不一样：写入redis，写入action表，调grpc接口
//考虑多线程处理
@Async("taskExecutor-monitor")
@Override
public void saveDeviceStatus(DeviceIdIplist.DeviceIdIp deviceIdIp) {
    boolean flag = getPingResult(deviceIdIp);
    int flagInt = flag ? 1 : 0;
    String redisFlag = stringRedisTemplate.opsForValue().get(RedisConst.DEVICE_CONNECT_STATUS + deviceIdIp.getDeviceId());
    if(!String.valueOf(flagInt).equals(redisFlag)){
        stringRedisTemplate.opsForValue().set(RedisConst.DEVICE_CONNECT_STATUS + deviceIdIp.getDeviceId(),String.valueOf(flagInt));

        DeviceConnectStatus deviceConnectStatus = DeviceConnectStatus.newBuilder()
                .setDeviceId(deviceIdIp.getDeviceId())
                .setStatus(flagInt)
                .build();
        deviceInfoStub.updateStatus(deviceConnectStatus);
    }
}

```

### 3.java8的Function函数写模版模式
Java 8在java.util.function下面增加增加一系列的函数接口。其中主要有Consumer、Supplier、Predicate、Function等。

这几个接口都在 java.util.function 包下的，分别是Consumer/ThrowingConsumer（消费型）、supplier（供给型）、predicate（谓词型）、function（功能性）


```.java
package com.uih.uplus.solar.log.parser.service;

import java.util.function.Function;

/**
 * @Description:
 * @Date: 2021/9/7 11:10
 */
public class FunctionTest {

        public static void  main(String[] args){
            System.out.println(compute(1,x-> (float) (2 * x)));
            System.out.println(compute(1,x-> (float) (2 * x + 3)));
        }

        private static Float compute(int num, Function<Integer, Float> function){
            Float result = function.apply(num);
            return result;
        }

}
```
链接：https://www.cnblogs.com/SIHAIloveYAN/p/11288064.html

## <span id="mysql">💾Mysql</span>

## <span id="elasticSearch">🎨ElasticSearch</span>
### 问题一
问题描述：分页查询场景，当查询记录数超过 10000 条时，会报错。  
原因分析：Elasticsearch 默认查询结果最多展示前 10000 条数据。  
解决方法：设置ES查询的最大返回数:
```xml
PUT _all/_settings {
  "index":{
    "max_result_window":5000000
   }
}
```
