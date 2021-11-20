package com.example.mobileftp;

import com.example.mobileftp.utils.AccountChecker;
import com.example.mobileftp.utils.FTPLogger;
import com.example.mobileftp.utils.Reply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class ServerThread extends Thread{
    private boolean usernameStatus = false;
    private boolean loginStatus = false;


    private Socket controlSocket = null;//控制连接
    private Socket dataSocket = null;//数据连接

    //指令和参数
    private String cmd;
    private String para;

    //命令输出流和命令输入流
    private BufferedReader cmdReader = null;
    private PrintWriter cmdWriter = null;

    //用户名和密码
    private String user = null;
    private String pass = null;

    private InetAddress clientAdd= null;
    public ServerThread(Socket socket) {
        this.controlSocket = socket;
        clientAdd = socket.getInetAddress();
    }

    @Override
    public void run() throws NullPointerException {
        String cmdLine; //客户端的命令
        try {
            //命令输入流和命令输出流
            cmdReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            cmdWriter = new PrintWriter(new ObjectOutputStream(controlSocket.getOutputStream()));

            //连接成功
            cmdWriter.println(new Reply(200, "Connect successfully").toString());
            FTPLogger.writeLog("Accepted " + clientAdd.getHostAddress()+":"+controlSocket.getPort()+":"+controlSocket.getLocalPort(),FTPLogger.INFO);

           while ((cmdLine = cmdReader.readLine())!=null) {
               FTPLogger.writeLog("commond:"+cmdLine,FTPLogger.INFO);
               StringTokenizer stringTokenizer = new StringTokenizer(cmdLine);
               cmd = stringTokenizer.nextToken();
               para = stringTokenizer.hasMoreTokens()? stringTokenizer.nextToken():"";

               switch (cmd) {
                   case "USER":
                       if(para == null || para == "") {
                           cmdWriter.println(new Reply(332).toString());
                       }
                       else {
                           if((new AccountChecker()).checkUser(para)) {
                               //user name valid
                               cmdWriter.println(new Reply(331).toString());
                               user = para;
                               usernameStatus = true;
                           }
                           else {
                               cmdWriter.println(new Reply(331).toString());
                           }
                       }
                       break;
                   case "PASS":
                       if(usernameStatus = true) {
                            if((new AccountChecker()).checkPass(user,para)) {
                                loginStatus = true;
                                FTPLogger.writeLog(clientAdd.getHostAddress()+":"+controlSocket.getPort()+":"+controlSocket.getLocalPort()+" user "+ user + "login.", FTPLogger.INFO);
                                cmdWriter.println(new Reply(230).toString());
                            }
                            else {
                                cmdWriter.println(new Reply(530).toString());
                            }
                       } else {
                           cmdWriter.println(new Reply(332).toString());
                       }


               }


           }
        } catch (IOException e) {
            e.printStackTrace();
            //错误处理待写
        }
    }
}
