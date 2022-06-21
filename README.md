

**我的分类:**
 
 Markdown 基本语法：https://www.cnblogs.com/real-l/p/9375476.html
 
Java相关 |Mysql|Elasticsearch|Kafka|dotNetCore|设计模式|JavaScript|Other
:---:|:---:|:-----------:|:-----------:|:-----------:|:-----------:|:-----------:|:-----------:|
[☕](#java)|[💾](#mysql)|[🎨](#Elasticsearch)|[💡](#Kafka)|[🔧](#dotNetCore)|[🍉](#designPattern)|[🍉](#JavaScript)|[🍉](#Other)|


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
 
**Spring boot配置线程池示例**
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

### 4，同一个类中，一个方法调用另外一个有注解（比如@Async，@Transational）的方法，注解失效的原因和解决方法
#### 4.1原因
spring 在扫描bean的时候会扫描方法上是否包含@Transactional注解，如果包含，spring会为这个bean动态地生成一个子类（即代理类，proxy），代理类是继承原来那个bean的。
此时，当这个有注解的方法被调用的时候，实际上是由代理类来调用的，代理类在调用之前就会启动transaction。
然而，如果这个有注解的方法是被同一个类中的其他方法调用的，那么该方法的调用并没有通过代理类，而是直接通过原来的那个bean，所以就不会启动transaction，我们看到的现象就是@Transactional注解无效。
#### 4.2解决方法
* 把这两个方法分开到不同的类中；
* 把注解加到类名上面；

### 5，Dubbo

Git：https://github.com/apache/dubbo

Dubbo Admin 运维指南：https://dubbo.incubator.apache.org/zh/docs/v2.7/admin/ops/

https://www.cnblogs.com/wangshouchang/p/9800659.html

### 6， BIO，NIO，AIO 

https://blog.csdn.net/m0_38109046/article/details/89449305

### 7，Java 中 IO 流
* 按功能来分：输入流（input）、输出流（output）
* 按类型来分：字节流和字符流
（字节流和字符流的区别是：字节流按8位传输以字节为单位输入输出数据，字符流按16位传输以字符为单位输入输出数据）

* 字节流操作的基本单元是字节；字符流操作的基本单元是字符
* 字节流默认不使用缓冲区；字符流使用缓冲区
* 字节流通常用于处理二进制数据，不支持直接读写字符；字符流通常用于处理文本数据
* 在读写文件需要对文本内容进行处理：按行处理、比较特定字符的时候一般会选择字符流；而仅仅读写文件，不处理内容，一般选择字节流

### 8， HashMap和Hashtable有什么区别？
* 存储：HashMap 允许 key 和 value 为 null，而 Hashtable 不允许
* 线程安全：Hashtable 是线程安全的，而 HashMap 是非线程安全的
* 推荐使用：在 Hashtable 的类注释可以看到，Hashtable 是保留类不建议使用，推荐在单线程环境下使用 HashMap 替代，如果需要多线程使用则用 ConcurrentHashMap 替代。

### 9， 自定义注解
https://blog.csdn.net/byteArr/article/details/103992016?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-4.no_search_link&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-4.no_search_link

### 10， spring中的@Resource与@Autowired用法区别
spring中，@Resource和@Autowired都是做bean的注入时使用。使用过程中，有时候@Resource和@Autowired可以替换使用；有时，则不可以。

**共同点：**  
@Resource和@Autowired都可以作为注入属性的修饰，在接口仅有单一实现类时，两个注解的修饰效果相同，可以互相替换，不影响使用。

**不同点：**  
@Resource是Java自己的注解，@Resource有两个属性是比较重要的，分是name和type；Spring将@Resource注解的name属性解析为bean的名字，而type属性则解析为bean的类型。所以如果使用name属性，则使用byName的自动注入策略，而使用type属性时则使用byType自动注入策略。如果既不指定name也不指定type属性，这时将通过反射机制使用byName自动注入策略。
@Autowired是spring的注解，是spring2.5版本引入的，Autowired只根据type进行注入，不会去匹配name。如果涉及到type无法辨别注入对象时，那需要依赖@Qualifier或@Primary注解一起来修饰。

## <span id="mysql">💾Mysql</span>
### 1，mybatis缓存机制

mybatis提供了缓存机制减轻数据库压力，提高数据库性能

mybatis的缓存分为两级：一级缓存、二级缓存

一级缓存是SqlSession级别的缓存，缓存的数据只在SqlSession内有效

二级缓存是mapper级别的缓存，同一个namespace公用这一个缓存，所以对SqlSession是共享的

![image](https://user-images.githubusercontent.com/39423273/134338447-de5de8e5-f0b0-47f4-b0f2-1dfe9ad9e5dd.png)

### 2，Mysql数据库的事务隔离
MySQL 的事务隔离是在 my.ini 配置文件里添加的，在文件的最后添加：
```xml
transaction isolation = REPEATABLE READ
```
可用的配置值：READ UNCOMMITTED、READ COMMITTED、REPEATABLE READ、SERIALIZABLE。
* **READ UNCOMMITTED**：未提交读，最低隔离级别、事务未提交前，就可被其他事务读取（会出现幻读、脏读、不可重复读）。
* **READ COMMITTED**：提交读，一个事务提交后才能被其他事务读取到（会造成幻读、不可重复读）。
* **REPEATABLE READ**：可重复读 **（默认级别）**，保证多次读取同一个数据时，其值都和事务开始时候的内容是一致，禁止读取到别的事务未提交的数据（会造成幻读）。
* **SERIALIZABLE**：串行化，代价最高最可靠的隔离级别，该隔离级别能防止脏读、不可重复读、幻读。

名词解释：
* **脏读**：表示一个事务能够读取另一个事务中还未提交的数据。比如，某个事务尝试插入记录 A，此时该事务还未提交，然后另一个事务尝试读取到了记录 A。
* **不可重复读**：是指在一个事务内，多次读同一数据。
* **幻读**：指同一个事务内多次查询返回的结果集不一样。比如同一个事务 A 第一次查询时候有 n 条记录，但是第二次同等条件下查询却有 n+1 条记录，这就好像产生了幻觉。发生幻读的原因也是另外一个事务新增或者删除或者修改了第一个事务结果集里面的数据，同一个记录的数据内容被修改了，所有数据行的记录就变多或者变少了。

查看和设置隔离级别：

1.连接mysql数据库
```xml
mysql -uroot -p
```
![image](https://user-images.githubusercontent.com/39423273/136648836-b1d17b44-2843-43bc-b029-aadd57c3a3c1.png)

2.查看系统当前隔离级别
```xml
SELECT @@global.tx_isolation
```
![image](https://user-images.githubusercontent.com/39423273/136648930-0b382054-c2a2-424c-bf8c-3b2c971a1137.png)

3.设置系统当前隔离级别
```xml
SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED
```
![image](https://user-images.githubusercontent.com/39423273/136649011-49ef13ab-3a30-48f6-9f0d-cb47f48dd24c.png)

### 3，mysql问题排查方法
**a. 使用 show (full) processlist 命令查看当前所有连接信息**
![image](https://user-images.githubusercontent.com/39423273/136741463-9324b395-745d-4de8-8bc4-01d428ebf2d9.png)

**b. 使用 explain 命令查看SQL语句执行情况**
执行sql语句：
```sql
explain select * from device where device.CUSTOMER_ID = 1376444245691330576
```
explain查看结果
![image](https://user-images.githubusercontent.com/39423273/136741591-02e93eba-4d50-48a0-a58b-3ad97b388c61.png)
explain分析查询：https://www.cnblogs.com/deverz/p/11066043.html

**c. 开启慢查询日志，查看慢查询的SQL**
* 打开慢查询日志的命令
```xml
set global slow_query_log=on;
```
![image](https://user-images.githubusercontent.com/39423273/136743054-c293c6eb-8e4e-425c-927e-9b15f4fdebdc.png)
* 设置sql语句执行两秒钟以上就写到慢查询日志中：
```xml
set global long_query_time=2;
```
设置完后要重新连接客户端才能看到设置后的long_query_time的值
* 查看设置
```xml
show variables like '%query%';
```
![image](https://user-images.githubusercontent.com/39423273/136743466-42e00b09-ffde-4cc6-b861-1c875a30c350.png)


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

## <span id="Kafka">💡Kafka</span>

Kafka是一个分布式流数据系统，使用Zookeeper进行集群的管理。与其他消息系统类似，整个系统由生产者、Broker Server和消费者三部分组成，生产者和消费者由开发人员编写，通过API连接到Broker Server进行数据操作。我们重点关注三个概念：

* Topic，是Kafka下消息的类别，类似于RabbitMQ中的Exchange的概念。这是逻辑上的概念，用来区分、隔离不同的消息数据，屏蔽了底层复杂的存储方式。对于大多数人来说，在开发的时候只需要关注数据写入到了哪个topic、从哪个topic取出数据。
* Partition，是Kafka下数据存储的基本单元，这个是物理上的概念。**同一个topic的数据，会被分散的存储到多个partition中**，这些partition可以在同一台机器上，也可以是在多台机器上，比如下图所示的topic就有4个partition，分散在两台机器上。这种方式在大多数分布式存储中都可以见到，比如MongoDB、Elasticsearch的分片技术，其优势在于：有利于水平扩展，避免单台机器在磁盘空间和性能上的限制，同时可以通过复制来增加数据冗余性，提高容灾能力。为了做到均匀分布，通常partition的数量通常是Broker Server数量的整数倍。
* Consumer Group，同样是逻辑上的概念，是Kafka实现单播和广播两种消息模型的手段。同一个topic的数据，会广播给不同的group；同一个group中的worker，只有一个worker能拿到这个数据。换句话说，对于同一个topic，每个group都可以拿到同样的所有数据，但是数据进入group后只能被其中的一个worker消费。group内的worker可以使用多线程或多进程来实现，也可以将进程分散在多台机器上，worker的数量通常不超过partition的数量，且二者最好保持整数倍关系，因为Kafka在设计时假定了一个partition只能被一个worker消费（同一group内）。

![image](https://user-images.githubusercontent.com/39423273/132612362-49ca2962-2757-4041-b290-2f1d10cf1d9e.png)


链接：https://blog.csdn.net/cao1315020626/article/details/112590786?ops_request_misc=%257B%2522request%255Fid%2522%253A%2522163110137116780262550619%2522%252C%2522scm%2522%253A%252220140713.130102334..%2522%257D&request_id=163110137116780262550619&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~all~top_positive~default-1-112590786.first_rank_v2_pc_rank_v29&utm_term=Kafka&spm=1018.2226.3001.4187

Kafka Tools:https://www.kafkatool.com/download.html

Kafka Tools 配置
[图片]![image](https://user-images.githubusercontent.com/39423273/133402987-818da82d-b12a-45cd-b48b-05d4ed5ceea3.png)
[图片]![image](https://user-images.githubusercontent.com/39423273/133403007-b0b36e99-c715-40d4-97bb-f163c6cac173.png)
[图片]![image](https://user-images.githubusercontent.com/39423273/133403030-8548504f-3787-4480-990b-96692600094b.png)
[图片]![image](https://user-images.githubusercontent.com/39423273/133403063-bfe5460e-935a-4c83-ba4a-ccdffa7c7e0a.png)

org.apache.kafka.common.security.plain.PlainLoginModule required username="consumer" password="cons-2019";

## <span id="dotNetCore">💡dotNetCore</span>

https://www.cnblogs.com/Fengyinyong/category/1854797.html

### 1.Websocket&Http
WebSocket是一种在单个TCP连接上进行全双工通信的协议.

HTTP 协议有一个缺陷：通信只能由客户端发起，做不到服务器主动向客户端推送信息。

WebSocket 协议 它的最大特点就是，服务器可以主动向客户端推送信息，客户端也可以主动向服务器发送信息，是真正的双向平等对话，属于服务器推送技术的一种

### 2.SignalR

https://www.cnblogs.com/yaopengfei/p/9276234.html

### 3.dnSpy反编译工具调试netcore项目

https://www.cnblogs.com/Bruce_H21/p/12307182.html

## <span id="designPattern">🍉设计模式</span>
设计模式（Design pattern）代表了最佳的实践，通常被有经验的面向对象的软件开发人员所采用。设计模式是软件开发人员在软件开发过程中面临的一般问题的解决方案。这些解决方案是众多软件开发人员经过相当长的一段时间的试验和错误总结出来的。

设计模式是一套被反复使用的、多数人知晓的、经过分类编目的、代码设计经验的总结。使用设计模式是为了重用代码、让代码更容易被他人理解、保证代码可靠性。

### 1.策略模式
在策略模式（Strategy Pattern）中，一个类的行为或其算法可以在运行时更改。这种类型的设计模式属于行为型模式。
在策略模式中，我们创建表示各种策略的对象和一个行为随着策略对象改变而改变的 context 对象。策略对象改变 context 对象的执行算法

#### 介绍
* 意图：定义一系列的算法,把它们一个个封装起来, 并且使它们可相互替换。  
* **主要解决：在有多种算法相似的情况下，使用 if...else 所带来的复杂和难以维护。**  
* 何时使用：一个系统有许多许多类，而区分它们的只是他们直接的行为。  
* 如何解决：将这些算法封装成一个一个的类，任意地替换。  
* 关键代码：实现同一个接口。  
* 应用实例：（1）诸葛亮的锦囊妙计，每一个锦囊就是一个策略;（2）旅行的出游方式，选择骑自行车、坐汽车，每一种旅行方式都是一个策略;（3）JAVA AWT 中的 LayoutManager。  
* **优点：（1）算法可以自由切换;（2）避免使用多重条件判断;（3）扩展性良好。**  
* **缺点：（1）策略类会增多;（2）所有策略类都需要对外暴露。**  
* 使用场景：（1）如果在一个系统里面有许多类，它们之间的区别仅在于它们的行为，那么使用策略模式可以动态地让一个对象在许多行为中选择一种行为。（2）一个系统需要动态地在几种算法中选择一种。（3）如果一个对象有很多的行为，如果不用恰当的模式，这些行为就只好使用多重的条件选择语句来实现。  
注意事项：如果一个系统的策略多于四个，就需要考虑使用混合模式，解决策略类膨胀的问题。

#### 项目实现示例
a.创建一个接口  
NotifyService.java
```java
public interface NotifyService {
    /**
     * 通知方法
     * @param userAddressMap 要通知的对象列表key: 用户名value：邮箱地址或者企业微信地址或者电话号码
     * @param content  通知的内容
     */
    Integer notify(Map<String, String> userAddressMap, String content);
}
```
b.创建实现接口的实体类  
(1)EmailServiceImpl.java
```java
public class EmailServiceImpl implements NotifyService {
  
    Integer notify(Map<String, String> userAddressMap, String content){
      //实现邮件告警业务
    };
}
```
(2)EnterpriseWechatServiceImpl.java
```java
public class EnterpriseWechatServiceImpl implements NotifyService {
  
    Integer notify(Map<String, String> userAddressMap, String content){
      //实现企业微信告警业务
    };
}
```
(3)MobilePhoneServiceImpl.java
```java
public class MobilePhoneServiceImpl implements NotifyService {
  
    Integer notify(Map<String, String> userAddressMap, String content){
      //实现电话告警业务
    };
}
```
c.创建Context类
NotifyContext.java
```java
public class NotifyContext {
    private Map<NotifyType, NotifyService> notifyServiceMap = new HashMap<>();

    @Autowired
    private EmailServiceImpl emailServiceImpl;

    @Autowired
    private EnterpriseWechatServiceImpl enterpriseWechatServiceImpl;
    
    @Autowired
    private MobilePhoneImpl mobilePhoneImpl;

   //发送方式的枚举
    public enum NotifyType {
        EMAIL,
        EnterpriseWechat,
        MobilePhone
    }

    @PostConstruct
    public void register() {
        notifyServiceMap.put(NotifyType.EMAIL, emailServiceImpl);
        notifyServiceMap.put(NotifyType.EnterpriseWechat, enterpriseWechatServiceImpl);
        notifyServiceMap.put(NotifyType.MobilePhone, mobilePhoneImpl);
    }

    public Integer send(NotifyType notifyType, Map<String, String> userAddressMap, String content) {
        NotifyService notifyService = notifyServiceMap.getOrDefault(notifyType, null);

        return notifyService.notify(userAddressMap, content);
    }

}
```
d.调用
```java
@Autowired
private NotifyContext notifyContext;

switch (alertPriorityEnum) {
    case NORMAL:  //发邮件             
        emailSendStatus = notifyContext.send(NotifyContext.NotifyType.EMAIL, userAddressMap, contentForSourceOfInfo);
        break;
    case HIGH: //发邮件和企业微信           
        emailSendStatus = notifyContext.send(NotifyContext.NotifyType.EMAIL, userAddressMap, contentForSourceOfInfo);
        enterpriseWechatSendStatus = notifyContext.send(NotifyContext.NotifyType.EnterpriseWechat, userAddressMap,contentForSourceOfInfo);
        break;
    case CRITICAL: //发邮件和企业微信和电话             
        emailSendStatus = notifyContext.send(NotifyContext.NotifyType.EMAIL, userAddressMap, contentForSourceOfInfo);
        enterpriseWechatSendStatus = notifyContext.send(NotifyContext.NotifyType.EnterpriseWechat, userAddressMap,contentForSourceOfInfo);
        mobilePhoneSendStatus = notifyContext.send(NotifyContext.NotifyType.MobilePhone, userAddressMap,contentForSourceOfInfo);
        break;
    default:
}
```
## <span id="JavaScript">🍉JavaScript</span>
### 1.ES6新特性
* **let 与 const**
* **解构赋值**
* **Symbol**: ES6引入了一种新的原始数据类型Symbol，表示独一无二的值，最大的用法是用来定义对象的唯一属性名。ES6 数据类型除了Number、String、Boolean、Object、null和undefined，还新增了Symbol。
* **Map与Set**
* **Reflect与Proxy**: (1)Proxy与Reflect是ES6为了操作对象引入的API;(2)Proxy 可以对目标对象的读取、函数调用等操作进行拦截，然后进行操作处理。它不直接操作对象，而是像代理模式，通过对象的代理对象进行操作，在进行这些操作时，可以添加一些需要的额外操作;(3)Reflect 可以用于获取目标对象的行为，它与 Object 类似，但是更易读，为操作对象提供了一种更优雅的方式。它的方法与 Proxy 是对应的。
* **新增字符串方法**
* **数值表示新方法**
* **对象新方法**
* **数组新方法**
* **ES6函数**
* **class类**
* **模块import/export**
* **Promise对象**
* **Generator函数(为异步编程提供解决方案)**
* **async函数**
### 2.Vue

https://cn.vuejs.org/

#### 2.1 Vue生命周期

![生命周期](https://user-images.githubusercontent.com/39423273/149468400-1a66c422-8a20-4a35-a8b9-d610ca3cb78d.png)


### 3.BootCDN

https://www.bootcdn.cn/


### 4.npmjs

https://www.npmjs.com/

### 5.前端组件——bootstrap table

## <span id="Other">🍉Other</span>

### 1,医学影像相关

https://dicom.innolitics.com/
