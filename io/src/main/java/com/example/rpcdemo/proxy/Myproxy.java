package com.example.rpcdemo.proxy;

import com.example.rpcdemo.rpc.Dispatcher;
import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.transport.ClientFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * @author alex
 * @date 2021-03-01 21:29
 * @descript
 */
public class Myproxy {


    public static <T> T getProxy(Class<T> interfaceInfo) {
        ClassLoader classLoader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};
        //实现 local or remote
        Dispatcher dis = Dispatcher.getDis();

        return (T) Proxy.newProxyInstance(classLoader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //consumer对于provider的调用过程
                // 1.调用服务，方法，参数  ==》 封装成message
                //利用Dispatcher 判断是本地执行还是远程执行
                Object result = null;
                Object o = dis.get(interfaceInfo.getName());
                if (o == null) {
                    //rpc调用
                    String name = interfaceInfo.getName();
                    String methodName = method.getName();
                    Class<?>[] parameterTypes = method.getParameterTypes();

                    MyContent myContent = new MyContent();
                    myContent.setArgs(args);
                    myContent.setMethodName(methodName);
                    myContent.setParameterTypes(parameterTypes);
                    myContent.setName(name);
                    //5.如果从IO回来了，怎么执行代码
                    CompletableFuture<Object> res = ClientFactory.transport(myContent);
                    result= res.get();//阻塞
                }else{
                    //local调用
                    System.out.println("local fc....");
                    Class<?> clazz = o.getClass();

                    Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
                    result = m.invoke(o, args);
                }

                return result;

            }
        });
    }



}
