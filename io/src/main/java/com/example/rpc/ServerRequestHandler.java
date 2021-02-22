package com.example.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author alex
 * @date 2021-02-22 0:28
 * @descript
 */
public class ServerRequestHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        PackMsg requestMsg = (PackMsg) msg;

//        System.out.println("server handler : "+requestMsg.content.getArgs()[0]);

        //把数据返回给客户端
        //btrebuf
        //因为是RPC，所以必须有requestID
        //在client那一侧也要解决解码问题
        //关注rpc 通信协议  来的时候flag 0x14141414


        String ioThreadName = Thread.currentThread().getName();
        //1.直接在当前方法处理io和业务流程 或者 2. 使用netty自己的eventloop来处理业务及返回  3.自己创建线程池
//        ctx.executor().execute(new Runnable(){
        ctx.executor().parent().next().execute(new Runnable(){

            @Override
            public void run() {
                //有新的header+content
                String execThreadName = Thread.currentThread().getName();
                MyContent content = new MyContent();

                String s = "io thread: " + ioThreadName + " exec thread: " + execThreadName + " from args: " + requestMsg.content.getArgs()[0];

                System.out.println(s);

                content.setRes(s);
                byte[] contentByte = SerDerUtil.ser(content);
                MyHeader resHeader = new MyHeader();
                resHeader.setRequestID(requestMsg.header.getRequestID());
                resHeader.setFlag(0x14141424);
                resHeader.setDataLen(contentByte.length);
                byte[] headerByte = SerDerUtil.ser(resHeader);
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(headerByte.length + contentByte.length);

                byteBuf.writeBytes(headerByte);
                byteBuf.writeBytes(contentByte);
                ctx.writeAndFlush(byteBuf);

            }
        });


    }
}
