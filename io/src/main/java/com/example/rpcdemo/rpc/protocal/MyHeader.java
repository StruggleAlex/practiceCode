package com.example.rpcdemo.rpc.protocal;

import java.io.Serializable;
import java.util.UUID;

/**
 * 通信协议头
 *  1.ooxx值
 *  2.uuid
 *  3.data_len
 */
public class MyHeader implements Serializable {

    /**
     * 32bit
     */
    int flag;

    long requestID;

    long dataLen;


    public int getFlag() {
        return flag;
    }

    public static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();
        int size = msg.length;
        long requestID = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        int f=0x14141414;
        //0x14 0001 0100
        header.setFlag(f);
        header.setDataLen(size);
        header.setRequestID(requestID);
        return header;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public long getDataLen() {
        return dataLen;
    }

    public void setDataLen(long dataLen) {
        this.dataLen = dataLen;
    }
}
