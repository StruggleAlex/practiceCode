package com.example.rpcdemo.rpc.srvice.impl;

import com.example.rpcdemo.rpc.srvice.Car;
import com.example.rpcdemo.rpc.srvice.Person;

/**
 * @author alex
 * @date 2021-03-01 22:36
 * @descript
 */
public class CarImpl implements Car {
    @Override
    public String run(String msg) {
        System.out.println("server ,get client arg: "+msg);
        return "server res " +msg;
    }

    @Override
    public Person getPerson(String name, Integer age) {
        Person p = new Person();
        p.setName(name);
        p.setAge(age);
        return p;
    }
}
