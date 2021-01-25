package com.example.testreactor;

public class MainThread {

    public static void main(String[] args) {
        //1.创建 IO Thread （一个或者多个）
        //SelectThreaGroup group = new SelectThreaGroup(1);
        //混杂模式， 只有一个线程负责accept，每个都会被分配client， 进行R/W
        SelectThreadGroup boss = new SelectThreadGroup(3);
        SelectThreadGroup worker = new SelectThreadGroup(3);

        //boss得多持有worker的引用
        boss.setWorker(worker);

        //2.把监听端口 (8888)的server注册到某一个selector 上
        /**
         * boss里选一个线程注册listen ， 触发bind，从而，这个不选中的线程得持有 workerGroup的引用
         * 因为未来 listen 一旦accept得到client后得去worker中 next出一个线程分配
         */
        boss.bind(8888);
        boss.bind(7777);
        boss.bind(6666);

    }
}
