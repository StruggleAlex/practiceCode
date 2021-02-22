package com.example.rpc;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1，先假设一个需求，写一个RPC
 * 2，来回通信，连接数量，拆包？
 * 3，动态代理呀，序列化，协议封装
 * 4，连接池
 * 5，就像调用本地方法一样去调用远程的方法，面向java中就是所谓的 面向interface开发
 */

public class MyRpcTest {

    @Test
    public void startServer() {
        NioEventLoopGroup boss = new NioEventLoopGroup(10);
        NioEventLoopGroup worker = boss;

        ServerBootstrap sbs = new ServerBootstrap();

        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        System.out.println("server accept client port:" + ch.remoteAddress().getPort());
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ServerDecode());
                        p.addLast(new ServerRequestHandler());
                    }
                }).bind(new InetSocketAddress("localhost", 9090));


        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * consumer
     */
    @Test
    public void get() {
        new Thread(()->{
            startServer();
        }).start();

        System.out.println("server started ....");


        AtomicInteger num = new AtomicInteger(0);
        int size = 20;
        Thread[] threads = new Thread[size];
        for (int i = 0; i <size; i++) {
            threads[i] = new Thread(()->{
                Car car = getProxy(Car.class);
                String arg = "car" + num.incrementAndGet();
                String res = car.run(arg);
                System.out.println("client over msg "+res+ " src arg: "+arg);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }


        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Bus bus = getProxy(Bus.class);
        bus.run("bus");*/
    }


    public static <T> T getProxy(Class<T> interfaceInfo) {
        ClassLoader classLoader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};

        return (T) Proxy.newProxyInstance(classLoader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //consumer对于provider的调用过程
                // 1.调用服务，方法，参数  ==》 封装成message
                String name = interfaceInfo.getName();
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                MyContent myContent = new MyContent();
                myContent.setArgs(args);
                myContent.setMethodName(methodName);
                myContent.setParameterTypes(parameterTypes);
                myContent.setName(name);

                //序列化
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream stream = new ObjectOutputStream(out);
                stream.writeObject(myContent);
                byte[] msgBody = out.toByteArray();

                //2.requestID +msg ，本地缓存
                //协议： 【header<>】 【msgbody】
                MyHeader header= createHeader(msgBody);

                out.reset();
                stream = new ObjectOutputStream(out);
                stream.writeObject(header);
                byte[] msgHeader = out.toByteArray();

                //3.连接池 :: get连接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel  clientChannel= factory.getClient(new InetSocketAddress("localhost",9090));

                //4.发送 -》通过 io -out （使用netty）
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
//                CountDownLatch countDownLatch = new CountDownLatch(1);
                CompletableFuture<String> res = new CompletableFuture<>();
                long requestID = header.getRequestID();
                ResponseMappingCallBack.addCallBack(requestID,res);
                byteBuf.writeBytes(msgHeader);
                byteBuf.writeBytes(msgBody);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);
                //io是双向的， sync 仅代表 out
                channelFuture.sync();
//                System.out.println("header len "+msgHeader.length);

//                countDownLatch.await();

                //5.如果从IO回来了，怎么执行代码

                return res.get();//阻塞
            }
        });
    }

    public static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();
        int size = msg.length;
        long requestID = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        int f=0x14141414;
        //0x14 0001 0100
        header.setFlag(f);
        header.setDataLen(size);
        header.setRequestID(requestID);
        return header;
    }


}





