package org.springframework.test.aop;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.CglibAopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.common.WorldServiceAfterAdvice;
import org.springframework.test.common.WorldServiceAfterReturningAdvice;
import org.springframework.test.common.WorldServiceBeforeAdvice;
import org.springframework.aop.GenericInterceptor;
import org.springframework.test.common.WorldServiceThrowsAdvice;
import org.springframework.test.service.WorldService;
import org.springframework.test.service.WorldServiceImpl;
import org.springframework.test.service.WorldServiceWithExceptionImpl;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class DynamicProxyTest {

    private AdvisedSupport advisedSupport;

    @Before
    public void setup() {
        WorldService worldService = new WorldServiceImpl();

        advisedSupport = new AdvisedSupport();
        TargetSource targetSource = new TargetSource(worldService);
        /*
            注意这里跟cglib-dynamic-proxy分支不一样了，之前是直接创建了一个WorldServiceInterceptor
            专门用于WorldService的方法增强，现在实现了一个通用的方法拦截器
            现在这个GenericInterceptor里面BeforeAdvice、AfterAdvice、AfterReturningAdvice、ThrowsAdvice属性都是空的
            所以如果现在执行proxy的方法的话，就相当于执行原方法
         */
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        MethodMatcher methodMatcher = new AspectJExpressionPointcut("execution(* org.springframework.test.service.WorldService.explode(..))").getMethodMatcher();
        advisedSupport.setTargetSource(targetSource);
        advisedSupport.setMethodInterceptor(methodInterceptor);
        advisedSupport.setMethodMatcher(methodMatcher);
    }

    @Test
    public void testJdkDynamicProxy() throws Exception {
        WorldService proxy = (WorldService) new JdkDynamicAopProxy(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testCglibDynamicProxy() throws Exception {
        WorldService proxy = (WorldService) new CglibAopProxy(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testProxyFactory() throws Exception {
        // 使用JDK动态代理
        advisedSupport.setProxyTargetClass(false);
        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();

        // 使用CGLIB动态代理
        advisedSupport.setProxyTargetClass(true);
        proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testBeforeAdvice() throws Exception {
        /*
            设置BeforeAdvice

            这里可以去看GenericInterceptor类的源码，如果没有设置BeforeAdvice，就没有前置增强方法
            要实现BeforeAdvice接口，重写里面的before方法
            把实现了BeforeAdvice的对象设置到GenericInterceptor里面
         */
        WorldServiceBeforeAdvice beforeAdvice = new WorldServiceBeforeAdvice();
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setBeforeAdvice(beforeAdvice);
        advisedSupport.setMethodInterceptor(methodInterceptor);

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testAfterAdvice() throws Exception {
        //设置AfterAdvice
        WorldServiceAfterAdvice afterAdvice = new WorldServiceAfterAdvice();
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setAfterAdvice(afterAdvice);
        advisedSupport.setMethodInterceptor(methodInterceptor);

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testAfterReturningAdvice() throws Exception {
        //设置AfterReturningAdvice
        WorldServiceAfterReturningAdvice afterReturningAdvice = new WorldServiceAfterReturningAdvice();
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setAfterReturningAdvice(afterReturningAdvice);
        advisedSupport.setMethodInterceptor(methodInterceptor);

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testThrowsAdvice() throws Exception {
        WorldService worldService = new WorldServiceWithExceptionImpl();
        //设置ThrowsAdvice
        WorldServiceThrowsAdvice throwsAdvice = new WorldServiceThrowsAdvice();
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setThrowsAdvice( throwsAdvice);
        advisedSupport.setMethodInterceptor(methodInterceptor);
        advisedSupport.setTargetSource(new TargetSource(worldService));

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }


    @Test
    public void testAllAdvice() throws Exception {
        //设置before、after、afterReturning
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setBeforeAdvice(new WorldServiceBeforeAdvice());
        methodInterceptor.setAfterAdvice(new WorldServiceAfterAdvice());
        methodInterceptor.setAfterReturningAdvice(new WorldServiceAfterReturningAdvice());
        advisedSupport.setMethodInterceptor(methodInterceptor);

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }

    @Test
    public void testAllAdviceWithException() throws Exception {
        WorldService worldService = new WorldServiceWithExceptionImpl();
        //设置before、after、throws
        GenericInterceptor methodInterceptor = new GenericInterceptor();
        methodInterceptor.setBeforeAdvice(new WorldServiceBeforeAdvice());
        methodInterceptor.setAfterAdvice(new WorldServiceAfterAdvice());
        methodInterceptor.setThrowsAdvice(new WorldServiceThrowsAdvice());
        advisedSupport.setMethodInterceptor(methodInterceptor);
        advisedSupport.setTargetSource(new TargetSource(worldService));

        WorldService proxy = (WorldService) new ProxyFactory(advisedSupport).getProxy();
        proxy.explode();
    }
}
















