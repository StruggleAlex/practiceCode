package com.example.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * @author alex
 * @date 2021-02-22 0:28
 * @descript
 */
public class ServerRequestHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        ByteBuf sendF = buf.copy();
        if (buf.readableBytes() >= 94) {
            byte[] bytes = new byte[94];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream stream = new ObjectInputStream(in);
            MyHeader header = (MyHeader) stream.readObject();
            System.out.println(header.dataLen);
            System.out.println("server requestID"+header.getRequestID());


            if (buf.readableBytes() >= header.getDataLen()) {
                byte[] data = new byte[(int) header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream dstream = new ObjectInputStream(din);
                MyContent content = (MyContent) dstream.readObject();
                System.out.println(content.getName());
            }
        }
        ChannelFuture channelFuture = ctx.writeAndFlush(sendF);
        channelFuture.sync();
    }
}
