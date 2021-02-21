package com.example.rpc;

import java.io.Serializable;

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
