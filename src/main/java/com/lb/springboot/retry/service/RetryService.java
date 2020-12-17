package com.lb.springboot.retry.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 *
 * 重试机制，不能在接口实现类里面写。所以要做重试，必须单独写个service。
 * @author lb
 */
@Service
public class RetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);

    /**
     * @Retryable的方法：
     * value：指定发生的异常进行重试
     * maxAttempts：重试次数
     * backoff：补偿策略
     * include：和value一样，默认空，当exclude也为空时，所有异常都重试
     * exclude：指定异常不重试，默认空，当include也为空时，所有异常都重试
     *
     * @return
     */
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
