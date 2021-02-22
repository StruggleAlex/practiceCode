package com.example.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 * @date 2021-02-22 0:15
 * @descript
 */
public class ResponseMappingCallBack {

   static ConcurrentHashMap<Long,CompletableFuture> mapping= new ConcurrentHashMap<>();


    public static void addCallBack(long requestID, CompletableFuture cb) {
        mapping.putIfAbsent(requestID, cb);
    }

    public  static  void  runCallBack(PackMsg packMsg) {
        CompletableFuture<String> cf = mapping.get(packMsg.getHeader().getRequestID());
        cf.complete(packMsg.getContent().getRes());
        remove(packMsg.getHeader().getRequestID());
    }

    private static void remove(long requestID) {
        mapping.remove(requestID);
    }

}
