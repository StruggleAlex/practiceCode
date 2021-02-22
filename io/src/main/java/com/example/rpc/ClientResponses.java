package com.example.rpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author alex
 * @date 2021-02-21 23:05
 * @descript
 */
public class ClientResponses  extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        PackMsg responsePkg = (PackMsg) msg;
        ResponseMappingCallBack.runCallBack(responsePkg);
    }
}
