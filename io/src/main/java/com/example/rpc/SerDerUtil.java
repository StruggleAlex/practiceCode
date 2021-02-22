package com.example.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author alex
 * @date 2021-02-22 21:31
 * @descript
 */
public class SerDerUtil {
  static    ByteArrayOutputStream out = new ByteArrayOutputStream();


    public synchronized static byte[] ser(Object msg) {
        out.reset();
        ObjectOutputStream stream = null;
        byte[] msgBody = null;
        try {
            stream = new ObjectOutputStream(out);
            stream.writeObject(msg);
            msgBody= out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msgBody;
    }
}
