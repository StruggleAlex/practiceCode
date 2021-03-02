package com.example.rpcdemo.rpc.transport;

import com.example.rpcdemo.util.PackMsg;
import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.protocal.MyHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * @author alex
 * @date 2021-02-22 20:53
 * @descript 解码器
 */
public class ServerDecode extends ByteToMessageDecoder {

    int headerLength = 111;
    /**
     * 父类里面一定有 channelread { 前边老的 拼接buf decode() ；剩余留存 ; 对out 遍历} --》 bytebuf
     * @param ctx
     * @param buf
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

//        System.out.println("channel start " + buf.readableBytes());
        while (buf.readableBytes() >= headerLength) {
            byte[] bytes = new byte[headerLength];
//            指针会移动
//            buf.readBytes(bytes);
//            指针不会移动
            buf.getBytes(buf.readerIndex(), bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream stream = new ObjectInputStream(in);
            MyHeader header = (MyHeader) stream.readObject();
//            System.out.println(header.dataLen);
//            System.out.println("server requestID"+header.getRequestID());

//            通信协议
            if (buf.readableBytes()-headerLength >= header.getDataLen()) {
//                处理指针
                buf.readBytes(headerLength);
                //移动指针到body开始的位置
                byte[] data = new byte[(int) header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream dstream = new ObjectInputStream(din);
//                System.out.println(content.getName());
                if(header.getFlag() == 0x14141414){
                    MyContent content = (MyContent) dstream.readObject();
                    out.add(new PackMsg(header,content));

                }else if(header.getFlag() == 0x14141424){
                    MyContent content = (MyContent) dstream.readObject();
                    out.add(new PackMsg(header,content));
                }

            }else {
                break;
            }
        }
    }
}
