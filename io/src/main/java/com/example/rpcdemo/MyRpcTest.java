package com.example.rpcdemo;


import com.example.rpcdemo.proxy.Myproxy;
import com.example.rpcdemo.rpc.Dispatcher;
import com.example.rpcdemo.rpc.srvice.Bus;
import com.example.rpcdemo.rpc.srvice.Car;
import com.example.rpcdemo.rpc.srvice.Person;
import com.example.rpcdemo.rpc.srvice.impl.BusImpl;
import com.example.rpcdemo.rpc.srvice.impl.CarImpl;
import com.example.rpcdemo.rpc.transport.ServerDecode;
import com.example.rpcdemo.rpc.transport.ServerRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
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
        CarImpl car = new CarImpl();
        BusImpl bus = new BusImpl();
        Dispatcher dis = Dispatcher.getDis();
        dis.register(Car.class.getName(),car);
        dis.register(Bus.class.getName(),bus);

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
                        p.addLast(new ServerRequestHandler(dis));
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
      /*  new Thread(()->{
            startServer();
        }).start();

        System.out.println("server started ....");*/


        AtomicInteger num = new AtomicInteger(0);
        int size = 20;
        Thread[] threads = new Thread[size];
        for (int i = 0; i <size; i++) {
            threads[i] = new Thread(()->{
                Car car = Myproxy.getProxy(Car.class);
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


    @Test
    public void testRpc() {
        Car car = Myproxy.getProxy(Car.class);
        Person person = car.getPerson("alex", 18);
        System.out.println(person);
    }


}





