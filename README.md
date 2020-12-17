# springboot-retry
# 前言
> 在我们日常开发中，会遇到比如A服务调用B服务RPC超时、获取锁失败等场景，我们需要进行重试操作，为了不侵入代码优雅的进行重试，我们可以利用`Spring`提供的`spring-retry`组件来实现。

# 开始
1. 引入依赖

核心依赖`spring-retry`，另外因为spring的retry底层是以AOP实现的，我们也需要引入`aspectj`
```XML
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>1.9.5</version>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>1.9.5</version>
        </dependency>
```
2. 开启`@EnableRetry`支持

`@EnableRetry`支持方法和类、接口、枚举级别的重试

```JAVA
@SpringBootApplication
@EnableRetry
public class SpringbootRetryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootRetryApplication.class, args);
    }
}
```
3.  编写重试service服务

`@Retryable`参数说明：
   * value：指定异常进行重试
   * maxAttempts：重试次数，默认3次
   * backoff：补偿策略
   * include：和value一样，默认空，当exclude也为空时，所有异常都重试
   * exclude：指定异常不重试，默认空，当include也为空时，所有异常都重试

```JAVA
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author lb
 */
@Service
public class RetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);

    @Retryable(
            // 指定发生的异常进行重试
            value = {RemoteAccessException.class, RemoteTimeoutException.class} ,
            // 重试次数
            maxAttempts = 3,
            // 补偿策略 delay：延迟多久执行补偿机制，multiplier：指定延迟的倍数，比如delay=5000,multiplier=2时，第一次重试为5秒后，第二次为10秒，第三次为20秒
            backoff = @Backoff(delay = 5000L,multiplier = 2)
    )
    public String call(){
        LOGGER.info("执行重试方法.....");
//        throw new RemoteAccessException("RPC访问异常");
        throw new RemoteTimeoutException("RPC调用超时异常");
    }

    @Recover
    public String recover(RemoteAccessException e){
        LOGGER.info("最终重试失败，执行RemoteAccess补偿机制 error : {}",e.getMessage());
        return "ok";
    }

    @Recover
    public String recover(RemoteTimeoutException e){
        LOGGER.info("最终重试失败，执行RemoteTimeout补偿机制 error : {}",e.getMessage());
        return "ok";
    }

}
```
有几点需要注意的地方：
* 重试机制的`service`服务必须单独创建一个`class`，不能写在接口的实现类里，否则会抛出ex
* `@Retryable`是以AOP实现的，所以如果`@Retryable`标记的方法被其他方法调用了，则不会进行重试。
* `recover`方法的返回值类型必须和`call()`方法的返回值类型一致

# 测试

* 编写测试类

```JAVA
import com.lb.springboot.retry.service.RetryService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringbootRetryApplicationTests {

    @Autowired
    private RetryService retryService;

    @Test
    public void testRetry(){
        String result = retryService.call();
        MatcherAssert.assertThat(result, Matchers.is("ok"));
    }

}
```
* 执行结果：
```BASH
 .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.4.RELEASE)

2020-12-17 14:15:58.083  INFO 71362 --- [           main] c.l.s.r.SpringbootRetryApplicationTests  : Starting SpringbootRetryApplicationTests on yunnashengdeMacBook-Pro.local with PID 71362 (started by yunnasheng in /Users/yunnasheng/work/github-workspace/springboot-retry)
2020-12-17 14:15:58.086  INFO 71362 --- [           main] c.l.s.r.SpringbootRetryApplicationTests  : No active profile set, falling back to default profiles: default
2020-12-17 14:15:59.238  INFO 71362 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-12-17 14:15:59.470  INFO 71362 --- [           main] c.l.s.r.SpringbootRetryApplicationTests  : Started SpringbootRetryApplicationTests in 1.686 seconds (JVM running for 2.582)

2020-12-17 14:15:59.668  INFO 71362 --- [           main] c.l.s.retry.service.RetryService         : 执行重试方法.....
2020-12-17 14:16:04.674  INFO 71362 --- [           main] c.l.s.retry.service.RetryService         : 执行重试方法.....
2020-12-17 14:16:14.675  INFO 71362 --- [           main] c.l.s.retry.service.RetryService         : 执行重试方法.....
2020-12-17 14:16:14.676  INFO 71362 --- [           main] c.l.s.retry.service.RetryService         : 最终重试失败，执行RemoteTimeout补偿机制 error : RPC调用超时异常
```
