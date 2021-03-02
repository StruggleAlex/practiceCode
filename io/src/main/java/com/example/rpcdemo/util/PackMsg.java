package com.example.rpcdemo.util;

import com.example.rpcdemo.rpc.protocal.MyContent;
import com.example.rpcdemo.rpc.protocal.MyHeader;

/**
 * @author alex
 * @date 2021-02-22 21:04
 * @descript 消息包
 */
public class PackMsg {

    MyHeader header;
    MyContent content;

    public PackMsg(MyHeader header, MyContent content) {
        this.header = header;
        this.content = content;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }
}
