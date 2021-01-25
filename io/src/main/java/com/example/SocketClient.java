package com.example;

import java.io.*;
import java.net.Socket;

public class SocketClient {

    public static void main(String[] args) {

        try {
            Socket client = new Socket("127.0.0.1",8080);

            client.setSendBufferSize(20);
            client.setTcpNoDelay(true);
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
