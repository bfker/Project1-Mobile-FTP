package com.example.mobileftp;

import java.net.Socket;

public class ServerThread extends Thread{

    private Socket controlSocket = null;//控制连接
    private Socket dataSocket = null;//数据连接

    public ServerThread(Socket socket) {
        this.controlSocket = socket;
    }

    @Override
    public void run() throws NullPointerException {
    }
}
