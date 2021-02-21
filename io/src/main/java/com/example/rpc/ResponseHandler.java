package com.example.rpc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 * @date 2021-02-22 0:15
 * @descript
 */
public class ResponseHandler {

   static ConcurrentHashMap<Long,Runnable> mapping= new ConcurrentHashMap<>();


    public static void addCallBack(long requestID, Runnable cb) {
        mapping.putIfAbsent(requestID, cb);
    }

    public  static  void  runCallBack(long requestID) {
        Runnable runnable = mapping.get(requestID);
        runnable.run();
        remove(requestID);
    }

    private static void remove(long requestID) {
        mapping.remove(requestID);
    }

}
