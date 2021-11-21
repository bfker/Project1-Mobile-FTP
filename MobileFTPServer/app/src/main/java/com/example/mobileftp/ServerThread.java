package com.example.mobileftp;

import android.os.Environment;

import com.example.mobileftp.utils.AccountChecker;
import com.example.mobileftp.utils.FTPLogger;
import com.example.mobileftp.utils.Reply;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class ServerThread extends Thread{
    private boolean usernameStatus = false;
    private boolean loginStatus = false;
    private boolean isAnonymous = false;



    private Socket controlSocket = null;//控制连接
    private Socket dataSocket = null;//数据连接

    private ServerSocket passDataSocket = null;
    //数据端口
    private int dataPort = -1;
    private final int defaultDataPort = 1089;

    private String clientDataIP = null;
    private int clientDataPort = -1;

    //指令和参数
    private String cmd;
    private String para;

    //文件根目录地址
    private String rootPath;

    //命令输出流和命令输入流
    private BufferedReader cmdReader = null;
    private PrintWriter cmdWriter = null;

    //用户名和密码
    private String user = null;
    private String psw = null;
    private String clientStr = null;

    private InetAddress clientAdd= null;
    private InetAddress localAdd= null;

    private File curDir = null;


    public ServerThread(Socket socket) {
        this.controlSocket = socket;
        clientAdd = socket.getInetAddress();
        localAdd = socket.getLocalAddress();
    }

    private Random random = new Random();

    @Override
    public void run() throws NullPointerException {
        init();
        //FTPLogger.writeLog("root path: " +  rootPath,FTPLogger.INFO);
        String cmdLine; //客户端的命令
        try {
            //命令输入流和命令输出流
            cmdReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            cmdWriter = new PrintWriter(new ObjectOutputStream(controlSocket.getOutputStream()));

            //连接成功
            writeCmd(new Reply(200, "Connect successfully").toString());

            clientStr = "Client " + clientAdd.getHostAddress()+":"+controlSocket.getPort();

            FTPLogger.writeLog("Accepted " + clientStr,FTPLogger.INFO);


            while ((cmdLine = cmdReader.readLine())!=null) {
               FTPLogger.writeLog(clientStr +" command:"+cmdLine,FTPLogger.INFO);
               StringTokenizer stringTokenizer = new StringTokenizer(cmdLine);
               cmd = stringTokenizer.nextToken();
               para = stringTokenizer.hasMoreTokens()? stringTokenizer.nextToken():"";
               switch (cmd) {
                   case "USER":
                       if(para == null || para == "") {
                           writeCmd(new Reply(332).toString());

                       }
                       else {
                           if(AccountChecker.isAnonymous(para)) {//匿名用户
                               isAnonymous = true;
                               usernameStatus = false;
                               loginStatus = false;
                               cmdWriter.println(new Reply(230, "User logged in as anonymous.").toString());
                               FTPLogger.writeLog("Login as anonymous user. " + clientStr, FTPLogger.INFO);
                           }
                           else
                           if((new AccountChecker()).checkUser(para)) {
                               //user name valid
                               writeCmd(new Reply(331).toString());

                               user = para;
                               isAnonymous = false;
                               loginStatus = false;
                               usernameStatus = true;
                           }
                           else {
                               writeCmd(new Reply(331).toString());
                           }
                       }
                       break;
                   case "PASS":
                       if(usernameStatus = true) {
                            if((new AccountChecker()).checkPass(user,para)) {
                                isAnonymous = false;
                                loginStatus = true;
                                psw = para;
                                FTPLogger.writeLog("User \""+ user + "\" login. " + clientStr, FTPLogger.INFO);
                                writeCmd(new Reply(230).toString());

                            }
                            else {
                                writeCmd(new Reply(530).toString());
                            }
                       } else {
                           writeCmd(new Reply(332).toString());
                       }
                       break;
                   case "PASV": //被动模式
                       if(!loginStatus) {//未登录
                           writeCmd(new Reply(530).toString());
                       }
                       else {
                           while (true) {
                               dataPort = getRandomPort();
                               try {
                                   passDataSocket = new ServerSocket(dataPort);
                                   curDir = new File(rootPath);
                                   break;
                               } catch (IOException e) {}
                           }
                           String desc = "Entering Passive Mode. " + getHostPortStr(dataPort);
                           writeCmd(new Reply(227, getHostPortStr(dataPort)).toString());
                           FTPLogger.writeLog(desc + clientStr, FTPLogger.INFO);
                       }
                       break;
                   case "PORT":
                       if(!loginStatus) {//未登录
                           writeCmd(new Reply(530).toString());
                       }
                       else if(para == null || para == "") { //参数为空
                           writeCmd(new Reply(501).toString());
                       }
                       else {
                            try{
                                dataPort = defaultDataPort;//设置成主动模式默认端口
                                if(analyseHostPortStr(para)) {
                                    FTPLogger.writeLog("ip="+clientDataIP + " port= "+clientDataPort ,FTPLogger.INFO);
                                    try {
                                        dataSocket = new Socket(clientDataIP,clientDataPort);
                                        writeCmd(new Reply(200).toString());
                                        FTPLogger.writeLog("Entering Passive Mode. " + clientStr, FTPLogger.INFO);
                                    } catch (Exception e) {
                                        writeCmd(new Reply(501).toString());
                                    }
                                }
                                else { //参数错误
                                    writeCmd(new Reply(501).toString());
                                }

                            } catch (Exception e) {
                                writeCmd(new Reply(501).toString());
                                FTPLogger.writeLog("wrong para", FTPLogger.INFO);
                            }
                       }
               }
           }
        } catch (IOException e) {
            e.printStackTrace();
            //错误处理待写
        }
    }

    private void init() {
        rootPath = Environment.getExternalStorageDirectory().getPath();
    }

    private int getRandomPort() {
        int port = 1024 + random.nextInt(65535-1024);
        return port;
    }

    private String getHostPortStr(int port) {
        String str = localAdd.getHostAddress().replace(".", ",");
        str = str + "," +((port >> 8) & 0xFF) + ","  + (port & 0xFF);
        return str;
    }

    private void writeCmd(String str) {
        cmdWriter.println(str);
        cmdWriter.flush();
    }

    private boolean analyseHostPortStr(String str){
        try {
            StringTokenizer st = new StringTokenizer(str,",");
            String ip = "";
            for(int i=0; i<4; i++) {
                if(st.hasMoreTokens()) {
                    int num = Integer.parseInt(st.nextToken());
                    if(i==0) ip = ip + num;
                    else ip = ip + "." + num;
                }
                else return false;
            }
            int portHigh = Integer.parseInt(st.nextToken());
            int portLow = Integer.parseInt(st.nextToken());
            clientDataIP = ip;
            clientDataPort = portHigh*256+portLow;
        } catch (Exception e) {
            return false;
        }

        return true;
    }



}
