package com.example.rpcdemo.rpc.srvice;

public interface Car {


    String run(String msg);

    Person getPerson(String name, Integer age);
}
