package com.example.rpcdemo.rpc.transport;

import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author alex
 * @date 2021-02-21 22:31
 * @descript 连接池
 */
public class ClientPool {

    NioSocketChannel[] clients;

    Object[] lock;

    ClientPool(int size) {
        //初始化 连接
        clients = new NioSocketChannel[size];
        //初始化 锁
        lock = new Object[size];
        for (int i = 0; i <size ; i++) {
            lock[i] = new Object();
        }
    }
}
