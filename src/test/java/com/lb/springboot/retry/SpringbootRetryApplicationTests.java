package com.lb.springboot.retry;

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
