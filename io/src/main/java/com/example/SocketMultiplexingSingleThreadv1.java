package com.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThreadv1 {

    private ServerSocketChannel server = null;
    //linux 多路复用器，（select poll epoll kqueue ) nginx event{}
    private Selector selector = null;

    int port = 8080;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //如果在epoll模型下，open -》 epoll_create -> fd3
            selector = Selector.open();

            //server 约等于 listen 状态的
            /**
             * register
             * 如果，select,poll,jvm里开辟一个数组， fd4 放进去
             * epoll，epoll_ctl(fd3,Add,fd4,EPOLLIN)
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void start() {
        initServer();
        System.out.println("服务器启动了....");
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");

                //1.调用多路复用器（select,poll or epoll (epoll_wait)）
                /**
                 * select()
                 * 1.select ，poll 其实 内核的select (fd4) poll(fd4)
                 * 2.epoll, 其实 内核的 epoll_wait()
                 * 参数可以带时间，没时间 0 ：阻塞，有时间设置一个超时
                 * selector.wakeup() 结果返回0
                 *
                 * 赖加载
                 * 其实再碰到selector。select() 调用的时候触发了epoll_ctl的调用
                 */

                while (selector.select() > 0) {
                    //返回的有状态的fd集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    /**
                     * 获取了状态，自己去处理R/W.
                     * Nio 自己对着每一个fd调用系统调用，浪费资源，
                     *
                     */
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        // 移除重复处理的元素
                        iter.remove();
                        if (key.isAcceptable()) {
                            /**
                             * select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                             * epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间
                             */
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            /**
                             * read 还有 write都处理了
                             * tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                             */
                            readHandler(key);

                        }
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端  fd7
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);  //前边讲过了

            // 0.0  我类个去
            //你看，调用了register
            /*
            select，poll：jvm里开辟一个数组 fd7 放进去
            epoll：  epoll_ctl(fd3,ADD,fd7,EPOLLIN
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}
