package com.example.testreactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 每个下次对应一个selector
 * 多线程下，该主机，该程序的并发客户端被分配到多个selector上
 * 注意，每个客户端，只绑定到其中一个selector ，不会产生交互问题
 */
public class SelectorThread implements Runnable{

    Selector selector = null;

    SelectThreadGroup selectThreadGroup;
    /**
     * 队列  堆里的对象，线程的栈是独立的，堆是共享的
     */
    LinkedBlockingDeque<Channel> lbq = new LinkedBlockingDeque<>();

    public SelectorThread(SelectThreadGroup stg) {
        try {
            this.selectThreadGroup = stg;
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                //1.select  阻塞 wakeup
//                System.out.println(Thread.currentThread().getName()+"   :  before select...."+ selector.keys().size());
                int nums = selector.select();  //阻塞  wakeup()
//                System.out.println(Thread.currentThread().getName()+"   :  after select...." + selector.keys().size());
                if (nums > 0) {
                    //2.處理selectKeys
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if(key.isReadable()){
                            readHander(key);
                        } else if (key.isWritable()) {

                        }


                    }
                }

                //3.处理一些task
                if (!lbq.isEmpty()) {
                    //只有方法的逻辑，本地的变量是线程隔离的
                    Channel c = lbq.take();
                    if (c instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) c;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println(Thread.currentThread().getName()+" register listen");
                    } else if (c instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) c;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                        System.out.println(Thread.currentThread().getName()+" register client: " + client.getRemoteAddress());

                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void readHander(SelectionKey key) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        while (true) {
            try {
                int num = client.read(buffer);
                //是否读取到
                if (num > 0) {
                    //读出写数据，
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                }else if (num<0){
                    //客户端断开 -1
                    System.out.println("client:"+client.getRemoteAddress()+" close....");
                    key.channel();
                    break;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName()+"   acceptHandler......");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            // 选择一个selector 并且注册
//            selectThreaGroup.nextSelector(client);
            selectThreadGroup.nextSelectorV2(client);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setWorker(SelectThreadGroup stgWorker) {
        this.selectThreadGroup = stgWorker;
    }
}
