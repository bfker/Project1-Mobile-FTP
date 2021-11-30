package com.example.mobileftp;

import com.example.mobileftp.utils.FTPLogger;
import android.os.StrictMode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class FTPServer {
    public void run() {
        FTPLogger.writeLog("Starting FTP Server...", FTPLogger.INFO);
        ServerSocket serverSocket = null;
        try {

           serverSocket = new ServerSocket(1088);
           //FTPLogger.writeLog((serverSocket.getLocalSocketAddress())+":"+serverSocket.getLocalPort(), FTPLogger.INFO);
        } catch (IOException e)  {
            FTPLogger.writeLog("Start FTP Server failed", FTPLogger.ERROR);
            e.printStackTrace();
            System.exit(1);
        }
        FTPLogger.writeLog("Start FTP Server successfully", FTPLogger.INFO);

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
            } catch (IOException e) {
                FTPLogger.writeLog("accept socket failed", FTPLogger.ERROR);
                e.printStackTrace();
            }
        }
    }
}
