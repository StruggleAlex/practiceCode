package com.example.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * @author alex
 * @date 2021-02-21 23:05
 * @descript
 */
public class ClientResponses  extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        if (buf.readableBytes() >= 94) {
            byte[] bytes = new byte[94];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream stream = new ObjectInputStream(in);
            MyHeader header = (MyHeader) stream.readObject();
            System.out.println(header.dataLen);
            System.out.println("client response "+header.getRequestID());
            ResponseHandler.runCallBack(header.requestID);

       /*     if (buf.readableBytes() >= header.getDataLen()) {
                byte[] data = new byte[(int)header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream dstream = new ObjectInputStream(in);
                MyContent content = (MyContent) stream.readObject();
                System.out.println(content.getName());
            }*/
        }
        super.channelRead(ctx, msg);
    }
}
