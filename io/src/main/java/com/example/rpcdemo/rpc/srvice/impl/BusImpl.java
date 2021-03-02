package com.example.rpcdemo.rpc.srvice.impl;

import com.example.rpcdemo.rpc.srvice.Bus;

/**
 * @author alex
 * @date 2021-03-01 22:37
 * @descript
 */
public class BusImpl implements Bus {
    @Override
    public void run(String msg) {
        System.out.println("server client bus "+msg);
    }
}
