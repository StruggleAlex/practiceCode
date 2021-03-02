package com.example.rpcdemo.rpc.srvice;

import java.io.Serializable;

/**
 * @author alex
 * @date 2021-03-02 21:16
 * @descript
 */
public class Person implements Serializable {

    String name;

    Integer age;

    public Person() {
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
