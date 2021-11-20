package com.example.mobileftp;

import com.example.mobileftp.utils.FTPLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class FTPServer {
    public void run() {
        FTPLogger.writeLog("Starting FTP Server...", FTPLogger.INFO);

/*        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(20);

        } catch (IOException e)  {
            FTPLogger.writeLog("Start FTP Server failed", FTPLogger.ERROR);
            e.printStackTrace();
            System.exit(1);
            //错误待写
        }
        FTPLogger.writeLog("Start FTP Server successfully", FTPLogger.INFO);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);
            } catch (IOException e) {
                e.printStackTrace();
                //错误待写
            }
        }*/
    }
}
