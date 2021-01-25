package com.example.testreactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectThreaGroup {
    /**
     * 线程组
     */
    SelectorThread[] group;
    /**
     * 服务端
     */
    ServerSocketChannel server;

    AtomicInteger xid = new AtomicInteger(0);


    /**
     *
     * @param num 线程数
     */
    public SelectThreaGroup(int num) {
        group = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            group[i]=new SelectorThread(this);

            new Thread(group[i]).start();

        }
    }

    /**
     * 绑定
     * @param port 端口号
     */
    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            //注册到哪一个selector上
            nextSelector(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private SelectorThread next() {
        int index = xid.incrementAndGet() % group.length;
        return group[index];
    }

    /**
     * 强制使用 thread-0去注册 listen
     * @return
     */
    private SelectorThread nextV2() {
        int index = xid.incrementAndGet() % (group.length-1);  //轮询就会很尴尬，倾斜
        return group[index+1];
    }

    public void nextSelectorV2(Channel channel) {
        try {
            if (channel instanceof ServerSocketChannel) {
                group[0].lbq.put(channel);
                group[0].selector.wakeup();
            }else {
                //在main 线程中，获取堆里的selectorThread 对象
                SelectorThread st = nextV2();
                //1.通过队列传递数据 消息
                st.lbq.add(channel);
                //2.通过打断阻塞，让对应的线程在打断后去完成注册的线程
                st.selector.wakeup();
            }
            } catch(InterruptedException e){
                e.printStackTrace();
            }
    }
    /**
     * 无论 serversocket socket 都可以复用此方法去选择selector
     *
     */
    public void nextSelector(Channel channel) {
        //在main 线程中，获取堆里的selectorThread 对象
        SelectorThread st = next();
        //1.通过队列传递数据 消息
        st.lbq.add(channel);
        //2.通过打断阻塞，让对应的线程在打断后去完成注册的线程
        st.selector.wakeup();




        // channel 可能是 server 有可能是client
      /*  ServerSocketChannel s = (ServerSocketChannel) channel;
        //呼应上，int nums= selector.select()  阻塞 wakeup（）
        try {
            st.selector.wakeup();
            //多线程下会产生阻塞
            s.register(st.selector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }*/

    }
}
