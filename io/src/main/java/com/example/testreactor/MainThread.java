package com.example.testreactor;

public class MainThread {

    public static void main(String[] args) {
        //1.创建 IO Thread （一个或者多个）
        //SelectThreaGroup group = new SelectThreaGroup(1);
        //混杂模式， 只有一个线程负责accept，每个都会被分配client， 进行R/W
        SelectThreaGroup group = new SelectThreaGroup(3);

        //2.把监听端口 (8888)的server注册到某一个selector 上
        group.bind(8888);
    }
}
