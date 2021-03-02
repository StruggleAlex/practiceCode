package com.example.rpcdemo;


import com.example.rpcdemo.proxy.Myproxy;
import com.example.rpcdemo.rpc.Dispatcher;
import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.srvice.Bus;
import com.example.rpcdemo.rpc.srvice.Car;
import com.example.rpcdemo.rpc.srvice.Person;
import com.example.rpcdemo.rpc.srvice.impl.BusImpl;
import com.example.rpcdemo.rpc.srvice.impl.CarImpl;
import com.example.rpcdemo.rpc.transport.MyHttpRpcHandler;
import com.example.rpcdemo.util.SerDerUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
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
                        //1,自定义rpc
//                        p.addLast(new ServerDecode());
//                        p.addLast(new ServerRequestHandler(dis));
                        //2. http方式
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(1024 * 512));
                        p.addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                              //http 协议 ，这个msg是一个 完整的 http-request
                                FullHttpRequest request = (FullHttpRequest) msg;
                                System.out.println(request);
                                //consumer 序列化的mycontent
                                ByteBuf buf = request.content();
                                byte[] data = new byte[buf.readableBytes()];
                                buf.readBytes(data);
                                ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(data));
                                MyContent myContent = (MyContent) oin.readObject();

                                String serviceName = myContent.getName();
                                String method = myContent.getMethodName();
                                Object c = dis.get(serviceName);
                                Class<?> clazz = c.getClass();
                                Object res = null;
                                try {


                                    Method m = clazz.getMethod(method, myContent.getParameterTypes());
                                    res = m.invoke(c, myContent.getArgs());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                MyContent resContent = new MyContent();
                                resContent.setRes(res);
                                byte[] contentByte = SerDerUtil.ser(resContent);

                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0,
                                        HttpResponseStatus.OK,
                                        Unpooled.copiedBuffer(contentByte));

                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH,contentByte.length);

                                //http协议，header+body
                                ctx.writeAndFlush(response);
                            }
                        });
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


    @Test
    public void startHttpServer() {
//        tomcat  jetty
        CarImpl car = new CarImpl();
        BusImpl bus = new BusImpl();

        Dispatcher dis = Dispatcher.getDis();

        dis.register(Car.class.getName(), car);
        dis.register(Bus.class.getName(), bus);


        //tomcat jetty  【servlet】
        Server server = new Server(new InetSocketAddress("localhost", 9090));
        ServletContextHandler handler = new ServletContextHandler(server, "/");
        server.setHandler(handler);
        //web.xml
        handler.addServlet(MyHttpRpcHandler.class,"/*");

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}





