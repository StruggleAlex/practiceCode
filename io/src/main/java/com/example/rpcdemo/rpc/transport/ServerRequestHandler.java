package com.example.rpcdemo.rpc.transport;

import com.example.rpcdemo.rpc.Dispatcher;
import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.protocal.MyHeader;
import com.example.rpcdemo.util.PackMsg;
import com.example.rpcdemo.util.SerDerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex
 * @date 2021-02-22 0:28
 * @descript
 */
public class ServerRequestHandler extends ChannelInboundHandlerAdapter {

    Dispatcher dis;

    public ServerRequestHandler(Dispatcher dis) {
        this.dis = dis;
    }

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
                String serviceName = requestMsg.getContent().getName();
                String method = requestMsg.getContent().getMethodName();
                Object c = dis.get(serviceName);
                Class<?> clazz = c.getClass();
                Object res = null;
                try {


                    Method m = clazz.getMethod(method, requestMsg.getContent().getParameterTypes());
                    res = m.invoke(c, requestMsg.getContent().getArgs());


                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                MyContent content = new MyContent();



                content.setRes(res);
                byte[] contentByte = SerDerUtil.ser(content);
                MyHeader resHeader = new MyHeader();
                resHeader.setRequestID(requestMsg.getHeader().getRequestID());
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
