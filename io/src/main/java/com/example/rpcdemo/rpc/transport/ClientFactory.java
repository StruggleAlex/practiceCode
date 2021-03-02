package com.example.rpcdemo.rpc.transport;

import com.example.rpcdemo.rpc.ResponseMappingCallBack;
import com.example.rpcdemo.util.SerDerUtil;
import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.protocal.MyHeader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 * @date 2021-02-21 22:35
 * @descript 连接工厂  一个consumer 可以连接多个provider ，每一个provider 都用自己的pool  k，v
 */
public class ClientFactory {

    int poolSize =5;

    ConcurrentHashMap<InetSocketAddress, ClientPool> outboxs = new ConcurrentHashMap<>();

   Random random= new Random();

    NioEventLoopGroup clientWorker;

    private static final ClientFactory factory;

    static {
        factory = new ClientFactory();
    }

    public static ClientFactory getFactory() {
        return factory;
    }

    public ClientFactory() {

    }

    public static CompletableFuture<Object> transport(MyContent content) {

        String type = "http";
        CompletableFuture<Object> res = new CompletableFuture<>();

        if (type.equals("rpc")) {
            byte[] msgBody = SerDerUtil.ser(content);

            //2.requestID +msg ，本地缓存
            //协议： 【header<>】 【msgbody】
            MyHeader header = MyHeader.createHeader(msgBody);
            byte[] msgHeader = SerDerUtil.ser(header);
            System.out.println("header length " + msgHeader.length);

            /**
             * 1，缺失了注册发现 zk
             * 2，第一层负载面相的provider
             * 3，consumer 线程池  面相 service ；并发就有木桶效应， 倾斜
             *  service A
             *         ipA：port
             *              socket1
             *              socket2
             *         ipB：port
             */
            //3.连接池 :: get连接
            NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));

            //4.发送 -》通过 io -out （使用netty）
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
//                CountDownLatch countDownLatch = new CountDownLatch(1);
            long requestID = header.getRequestID();
            ResponseMappingCallBack.addCallBack(requestID, res);
            byteBuf.writeBytes(msgHeader);
            byteBuf.writeBytes(msgBody);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);
        } else {
            //使用http 协议为载体
            //1，用URL 现成的工具（包含了http的编解码，发送，socket，连接）

            //2，自己编写 on netty （io 框架）+ 已经提供的http相关编码
        }


        return res;
    }

    public  NioSocketChannel getClient(InetSocketAddress address) {
        //优化并发
        ClientPool clientPool = outboxs.get(address);

        if (clientPool == null) {
            synchronized(outboxs){
                if (clientPool == null) {
                    outboxs.putIfAbsent(address, new ClientPool(poolSize));
                    clientPool = outboxs.get(address);
                }
            }
        }
        int i = random.nextInt(poolSize);

        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        }else {
            synchronized (clientPool.lock[i]) {
                if (clientPool.clients[i]==null  || !clientPool.clients[i].isActive()) {
                     clientPool.clients[i] = create(address);
                }
            }
        }

        return clientPool.clients[i];

    }


    private NioSocketChannel create(InetSocketAddress address) {

        //基于 netty的 客户端创建方式
        clientWorker = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(clientWorker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ServerDecode());
                        p.addLast(new ClientResponses());
                    }
                }).connect(address);

        try {
            NioSocketChannel client = (NioSocketChannel) connect.sync().channel();
            return client;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
