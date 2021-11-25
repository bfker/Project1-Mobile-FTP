package com.example.mobileftp;

import android.os.Environment;

import com.example.mobileftp.utils.AccountChecker;
import com.example.mobileftp.utils.FTPLogger;
import com.example.mobileftp.utils.Reply;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class ServerThread extends Thread{
    private boolean usernameStatus = false;
    private boolean loginStatus = false;
    private boolean isAnonymous = false;
    private boolean dataConnectOn = false;


    private Socket controlSocket = null;//控制连接
    private Socket dataSocket = null;//数据连接

    private ServerSocket dataSocketServer = null;
    //数据端口
    private int dataPort = -1;
    private final int defaultDataPort = 1089;

    private String clientDataIP = null;
    private int clientDataPort = -1;

    //指令和参数
    private String cmd;
    private String para;

    private String typeCode;
    private String modeCode;
    private String struCode;



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
            cmdWriter = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

            //
            writeCmd(new Reply(200, "Connect successfully").toString());

            clientStr = "Client " + clientAdd.getHostAddress()+":"+controlSocket.getPort();

            FTPLogger.writeLog("Accepted " + clientStr,FTPLogger.INFO);


            while ((cmdLine = cmdReader.readLine())!=null) {
               FTPLogger.writeLog(clientStr +" command:"+cmdLine,FTPLogger.INFO);
               StringTokenizer stringTokenizer = new StringTokenizer(cmdLine);
               cmd = stringTokenizer.nextToken();
               para = stringTokenizer.hasMoreTokens()? stringTokenizer.nextToken():"";
               switch (cmd.toUpperCase()) {
                   case "USER":
                       if(para == null || para == "") {
                           writeCmd(new Reply(332).toString());

                       }
                       else {
                           if(AccountChecker.isAnonymous(para)) {//匿名用户
                               isAnonymous = true;
                               usernameStatus = false;
                               loginStatus = false;
                               writeCmd(new Reply(230, "User logged in as anonymous.").toString());
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
                               writeCmd(new Reply(332).toString());
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
                       }
                       else if(isAnonymous) {
                           writeCmd(new Reply(230).toString()); //匿名登录
                       }
                       else {
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
                                   dataSocketServer = new ServerSocket(dataPort);
                                   dataConnectOn = true; //链接开启
                                   break;
                               } catch (IOException e) {}
                           }
                           String desc = "Entering passive mode. " + getHostPortStr(dataPort);
                           writeCmd(new Reply(227, getHostPortStr(dataPort)).toString());
                           FTPLogger.writeLog(desc + " " + clientStr, FTPLogger.INFO);
                           dataSocket = dataSocketServer.accept(); //连接
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
                                    try {
                                        FTPLogger.writeLog("Trying to connect "+clientDataIP+":"+ clientDataPort, FTPLogger.INFO);

                                        dataSocket = new Socket(clientDataIP,clientDataPort);
                                        //dataSocket.bind(new InetSocketAddress(dataPort));
                                       //dataSocket.connect(new InetSocketAddress(clientDataIP,clientDataPort));

                                        writeCmd(new Reply(200).toString());
                                        FTPLogger.writeLog("Entering active mode. " + clientStr, FTPLogger.INFO);
                                    } catch (Exception e) {
                                        FTPLogger.writeLog("Connect failed "+ e.toString(),FTPLogger.ERROR);
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
                       break;
                   case "LIST" ://匿名用户可以list
                       if((!loginStatus ) && (!isAnonymous)) { //此时数据连接已建立，dataSocket已存在
                           writeCmd(new Reply(530).toString());
                       }
                       else {
                           try {
                               String msg = "";
                               File[] fileList = new File(rootPath).listFiles();
                               for(File file : fileList) {
                                   msg = msg + " " + file.getName();
                               }
                               writeCmd("250"+msg);

                           } catch (Exception e) {
                               writeCmd(new Reply(451).toString());
                               FTPLogger.writeLog("List failed. " +e.toString() + clientStr, FTPLogger.ERROR);
                           }

                       }
                       break;
                   case "TYPE":
                       switch (para) {
                           case "I":
                           case "A":
                           case "E":
                           case "L":
                               setType(para);
                               break;
                           default:
                               writeCmd(new Reply(501).toString());
                               break;
                       }
                       break;
                   case "MODE":
                       switch (para) {
                           case "S":
                           case "B":
                           case "C":
                               setMode(para);
                               break;
                           default:
                               writeCmd(new Reply(501).toString());
                               break;
                       }
                       break;
                   case "STRU":
                       switch (para) {
                           case "F":
                           case "R":
                           case "P":
                               setStru(para);
                               break;
                           default:
                               writeCmd(new Reply(501).toString());
                               break;
                       }
                       break;
                   case "STOR"://只实现单一文件 客户端上传到服务器
                       if(!loginStatus) {//未登录
                           writeCmd(new Reply(530).toString());
                           break;
                       } else if(!dataConnectOn) { //数据连接未建立
                           writeCmd(new Reply(425).toString());
                           break;
                       }
                       try {
                           String pathName = para;
                           File file = new File(rootPath + "/" + pathName);
                           if(typeCode == "I") {//二进制
                               FileOutputStream fileOutput = new FileOutputStream(file);
                               DataInputStream dataReader = new DataInputStream(dataSocket.getInputStream());/*字节数据输出流*/
                               //告知一文件没有问题，可以开始传输
                               writeCmd(new Reply(125,"BINARY Data connection already open; transfer starting.").toString());

                               byte[] bytes = new byte[1024];
                               int len = 0;
                               while((len = dataReader.read(bytes, 0 ,bytes.length)) >= 0) {
                                   fileOutput.write(bytes, 0 ,len);
                                   fileOutput.flush();
                               }
                               fileOutput.close();
                               dataReader.close();
                               writeCmd(new Reply(226).toString());
                               FTPLogger.writeLog("Data connection closed. " + clientStr, FTPLogger.INFO);

                           } else { //ascii
                               BufferedWriter fileOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"ASCII"));
                               BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(),"ASCII"));

                               writeCmd(new Reply(125,"ASCII Data connection already open; transfer starting.").toString());

                               byte[] bytes = new byte[1024];
                               String line;
                               while((line = dataReader.readLine())!=null) {
                                   fileOutput.write(line);
                                   fileOutput.newLine();
                                   fileOutput.flush();
                               }
                               dataReader.close();
                               fileOutput.close();
                               writeCmd(new Reply(226).toString());
                               FTPLogger.writeLog("Data connection closed. " + clientStr, FTPLogger.INFO);

                           }
                           dataConnectOn = false;
                           dataSocket.close();
                           dataSocketServer.close();//???

                       } catch (Exception e) {
                           writeCmd(new Reply(451).toString());
                       }
                       break;
                   case "RETR"://服务器下载到客户端
                       if(!loginStatus) {//未登录
                           writeCmd(new Reply(530).toString());
                           break;
                       } else if(!dataConnectOn) { //数据连接未建立
                           writeCmd(new Reply(425).toString());
                           break;
                       }

                       String pathName = para;
                       try {
                           File file = new File(rootPath + "/" + pathName);
                           if(!file.exists()) { //不存在
                               writeCmd(new Reply(450).toString());
                           } else if(file.isFile()){//存在而且是文件
                               if(typeCode == "I") {//二进制
                                   FileInputStream fileInput = new FileInputStream(file);
                                   DataOutputStream dataWriter = new DataOutputStream(dataSocket.getOutputStream());/*字节数据输出流*/
                                   writeCmd(new Reply(125,"BINARY Data connection already open; transfer starting.").toString());

                                   byte[] bytes = new byte[1024];
                                   int len = 0;
                                   while((len = fileInput.read(bytes, 0 ,bytes.length)) >= 0) {
                                       dataWriter.write(bytes, 0 ,len);
                                       dataWriter.flush();
                                   }
                                   dataWriter.close();
                                   fileInput.close();
                                   writeCmd(new Reply(226).toString());
                               } else { //ascii
                                   BufferedReader fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(file),"ASCII"));
                                   BufferedWriter dataWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(),"ASCII"));
                                   writeCmd(new Reply(125,"ASCII Data connection already open; transfer starting.").toString());

                                   byte[] bytes = new byte[1024];
                                   String line;
                                   while((line = fileInput.readLine())!=null) {
                                       dataWriter.write(line);
                                       dataWriter.newLine();
                                       dataWriter.flush();
                                   }
                                   dataWriter.close();
                                   fileInput.close();
                                   writeCmd(new Reply(226).toString());
                               }
                               dataConnectOn = false;
                               dataSocket.close();
                               dataSocketServer.close();//???
                           } else {
                               writeCmd(new Reply(502).toString());
                           }
                       } catch (Exception e) {
                            writeCmd(new Reply(451).toString());
                       }
                    break;
                   case "NOOP":
                       writeCmd(new Reply(200).toString());
                       break;
                   case "QUIT":
                       init();
                       writeCmd(new Reply(221).toString());
                       cmdWriter.close();
                       cmdReader.close();
                       FTPLogger.writeLog(clientStr + " quit.",FTPLogger.INFO);
                       break;
                   default:
                       writeCmd(new Reply(500).toString());

               }
           }
        } catch (IOException e) {
            e.printStackTrace();
            //错误处理待写
        }
    }

    private void init() {
        dataConnectOn = false;
        usernameStatus = false;
        loginStatus = false;
        isAnonymous = false;

        typeCode = "I";
        modeCode = "S";
        struCode = "F";

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
        FTPLogger.writeLog("Server: " + str + " " +clientStr,FTPLogger.INFO);
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

    private void setType(String newTypeCode) {
        typeCode = newTypeCode;
        FTPLogger.writeLog("Set type "+typeCode+"."+clientStr,FTPLogger.INFO);
        writeCmd(new Reply(200).toString());
    }

    private void setMode(String newModeCode) {
        modeCode = newModeCode;
        FTPLogger.writeLog("Set mode "+modeCode+"."+clientStr,FTPLogger.INFO);
        writeCmd(new Reply(200).toString());
    }

    private void setStru(String newStruCode) {
        struCode = newStruCode;
        FTPLogger.writeLog("Set struct "+struCode+"."+clientStr,FTPLogger.INFO);
        writeCmd(new Reply(200).toString());
    }

}
