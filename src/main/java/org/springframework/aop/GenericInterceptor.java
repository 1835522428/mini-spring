package org.springframework.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * GenericInterceptor是在proxy执行被代理对象的方法时，实际执行的增强方法
 * 相当于cglib-dynamic-proxy分支的WorldServiceInterceptor
 *
 * 以JDK动态代理为例，proxy是WorldService的动态代理对象
 * proxy.explode()方法会直接跳转执行h.invoke()方法，这个h是InvocationHandler
 * 而在Spring编写的h.invoke()方法中（参考JdkDynamicAopProxy#invoke方法），转去执行了MethodInterceptor#invoke方法
 * 而所谓的MethodInterceptor#invoke方法就是本类里面的invoke
 * 所以这个GenericInterceptor#invoke就是代理对象proxy实际执行的增强方法
 * @author derekyi
 * @date 2020/12/6
 */
public class GenericInterceptor implements MethodInterceptor {
    private BeforeAdvice beforeAdvice;
    private AfterAdvice afterAdvice;
    private AfterReturningAdvice afterReturningAdvice;
    private ThrowsAdvice throwsAdvice;


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = null;
        try {
            // 前置通知
            if (beforeAdvice != null) {
                beforeAdvice.before(invocation.getMethod(), invocation.getArguments(), invocation.getThis());
            }
            //执行原方法，这里其实就是用反射执行的：method.invoke(target, arguments)
            result = invocation.proceed();
        } catch (Exception throwable) {
            //异常通知
            if (throwsAdvice != null) {
                throwsAdvice.throwsHandle(throwable, invocation.getMethod(), invocation.getArguments(), invocation.getThis());
            }
        } finally {
            //后置通知
            if (afterAdvice != null) {
                afterAdvice.after(invocation.getMethod(), invocation.getArguments(), invocation.getThis());
            }
        }
        // 返回通知
        if (afterReturningAdvice != null) {
            afterReturningAdvice.afterReturning(result, invocation.getMethod(), invocation.getArguments(), invocation.getThis());
        }
        return result;
    }

    public void setBeforeAdvice(BeforeAdvice beforeAdvice) {
        this.beforeAdvice = beforeAdvice;
    }

    public void setAfterReturningAdvice(AfterReturningAdvice afterReturningAdvice) {
        this.afterReturningAdvice = afterReturningAdvice;
    }

    public void setThrowsAdvice(ThrowsAdvice throwsAdvice) {
        this.throwsAdvice = throwsAdvice;
    }

    public void setAfterAdvice(AfterAdvice afterAdvice) {
        this.afterAdvice = afterAdvice;
    }
}
