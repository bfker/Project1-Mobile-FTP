

# Lab1 MobileFTP 说明文档

朱奕新 19307090029

谢知然 19302010085

## 测试运行环境

红米 note9 5G(Xiaomi M2007J22C)

Android 7.1.2

## 客户端使用方法

1. 下载client.apk并安装此应用。

2. 需保证server应用正在运行，且和本应用在同一个网段下。

3. 打开应用后首先进入的是登陆页面，

   登录页面输入服务器IP地址（默认连接1088端口）

   用户名和密码，点击Login按钮，即完成连接和登录（这里使用了USER、PASS命令）

4. 随后即跳转到main页面。在上方输入命令并按下Command按钮，即可向服务器发送命令。客户端的报错信息或服务器的回复信息会显示在按钮下方。

命令的格式及服务器的回复大致如下：

```
命令(<SP>参数)
响应码<sp>信息
```



## 服务器使用方法：

1. 下载server.apk并安装。

2. 保证客户端和服务器应用处在同一网段下。

3. 点击START开启服务器服务。

   服务器页面的Textview框显示从客户端受到的命令，执行的情况和返回结果。

   

## 客户端设计与实现

### 完成情况

- FTP Passvie Mode: 

  - 使用`PASV`命令，打开passive mode的数据连接
  - 使用`PORT`命令，打开active mode的数据连接

- File Transfer: 

  - 上传文件到服务器，`STOR filename`
  - 从服务器下载文件，`RETR filenam`

- Folder Transfer: 

  - 上传文件夹
  - 下载文件夹

- Robustness: 

  - 对于用户的非法输入，有toast提示，

  - 同时在按钮下方也会显示服务器的返回信息或错误信息

    

### 整体组织

客户端使用kotlin进行开发，包含：

- 两个activity，分别是登录页面`HomepageActivity`和主页面`MainActivity`以及其跳转逻辑。

- Client类，包含每个命令的具体操作和返回信息。

- tool下的InnerOperation类，对连接过程和每个命令对应的客户端操作进行了封装，将操作新开线程进行，并进行线程同步

  

### 实现细节

- **建立socket通信**

  ```kotlin
  @Throws(IOException::class)
      fun tryConnect(newAddress: String?, newPort: Int): Boolean {
          var success = false
          serverAddress = newAddress
          try {
              controlSocket = Socket(serverAddress, newPort)
              val outputStream = controlSocket!!.getOutputStream()
              controlwriter = PrintWriter(OutputStreamWriter(outputStream))
              val inputStream = controlSocket!!.getInputStream()
              controlreader = BufferedReader(InputStreamReader(inputStream))
              answer()
              when(currentResponse){
                  "200" -> {
                      state = UserStates.IN
                      success = true
                      init()
                  }
                  else -> success = false
              }
          } catch (e: IOException) {
              success = false
          }
          return success
      }
  ```

  因为在Android开发中，只有主线程才能更新客户端UI，但主线程却不能进行Socket网络通信，所以有必要调用其他Thread用于和服务器通信。

- **线程同步**

  InnerOperation将各个命令的具体操作都新开线程进行处理，解决了主线程不能进行Socket通信的问题

  使用` Thread.join`进行同步，使得函数能返回正确的返回值。

  ```kotlin
  @JvmStatic
  fun connect(passUser: String?): Boolean {
      success = false
      val t = Thread {
          try {
              if (myClient!!.haslogin) {
                  myClient!!.closeConnection()
              }
              if (myClient!!.tryConnect(serverAddress, port)) {
                  if (myClient!!.tryLogin(username!!, password!!)) {
                      success = true
                  }
              }
  
          } catch (e: IOException) {
              success = false
          }
      }
      t.start()
      t.join(5000)
      return success
  }
  ```

- 更新UI

  在线程同步之后，即可获得函数的正确返回值

  这里将主界面显示的信息提示内容作为返回值返回到`MainActivity`，这样就能从主线程实时更新UI



## 服务器设计与实现

### 完成情况

- Authorization:

  - 匿名用户

    用户名为anonymous，密码为任意值，既可以作为匿名用户登录

    匿名用户可以使用LIST、TYPE、MODE、STRU、QUIT、NOOP命令

    不能建立数据连接或传输文件

  - 合法用户

    内置两个合法用户，`test/test`、`admin/123456`

    合法用户可以使用全部命令

  - 非法用户

    非法用户名和密码不能连接服务器，不能登录

- File Transfer: 

  - 上传文件到服务器的请求，`STOR filename`
  - 从服务器下载文件的请求，`RETR filenam`

- Folder Transfer: 

  - 处理文件夹的下载请求
  - 处理文件夹的上传请求

- Robustness: 

  - 能处理用户的非法输入，传回相应的响应码和message

  - 服务器会在页面中Textview框显示错误，

    

### 整体组织

服务器使用java进行开发，服务器包含一个MainActivity，也只有这一个界面

- 主要的类：

  - FTPServer

    建立serverSocket进行监听，不断接入连接进来的客户端，

    对每一个与服务器链接的客户端socket，新开ServerThread线程对其提供服务

  - ServerThread

    为一个客户端socket提供服务的线程。继承自Thread类，重写`run()`处理来自客户端的命令。

- 工具类

  - AccountChecker：封装了用户名和密码，以及是否为匿名用户的检验
  - FTPLogger：封装了log的生成，对服务器操作进行记录
  - LogRefresher：将log的内容更新到前端页面
  - Reply：封装了响应码及其对应的message
  - StartBtnListener：继承自 View.OnClickListener，监听按钮的动作，对按钮被按下事件进行处理

### 实现细节

- 建立socket通信

  ```java
  //FTPServer类
  public class FTPServer {
      public void run() {
          FTPLogger.writeLog("Starting FTP Server...", FTPLogger.INFO);
          ServerSocket serverSocket = null;
          try {
             serverSocket = new ServerSocket(1088);
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
  
  //ServerThread类初始化
  public class ServerThread extends Thread{
     ...
      public ServerThread(Socket socket) {
          this.controlSocket = socket;
          clientAdd = socket.getInetAddress();
          localAdd = socket.getLocalAddress();
      }
  	...
  
  ```

  由于在Android开发中，UI只能被主线程更新，但主线程不能进行Socket通信，但需要在点击按钮的时候开启服务器服务，

  因此用StartBtnListener继承按钮的点击事件监听器，在按钮被点击时新建线程开启FTP服务器。

  ```java
  public class StartBtnListener implements View.OnClickListener {
      private Button startBtn;
      private FTPServer ftpServer;
      public StartBtnListener(Button button, FTPServer server) {
          super();
          this.startBtn = button;
          this.ftpServer = server;
      }
      @Override
      public void onClick(View view) {
          startBtn.setEnabled(false);
          Toast.makeText(view.getContext(),"you click the startBtn",Toast.LENGTH_SHORT).show();
          new Thread(new Runnable() {
              @Override
              public void run() {
                  ftpServer.run();
              }
          }).start();
      }
  }
  ```

- Log内容实时更新到TextView

  由于只有主线程能够更新UI，于是使用View.post(Runnable r)

  从Runnable派生子类，重载run()方法。然后调用View.post()即把Runnable对象增加到UI线程中运行。

  ```java
  public class LogRefresher {
      private static TextView textView = null;
      public LogRefresher(TextView tv) {
          textView = tv;
          tv.setMovementMethod(ScrollingMovementMethod.getInstance());
      }
  
      public static void refresh(String newLog) {
          if(textView != null) {
              textView.post(new Runnable() {
                  @Override
                  public void run() {
                      textView.append(newLog);
                      int scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();
                      if (scrollAmount > 0)
                          textView.scrollTo(0, scrollAmount);
                      else
                          textView.scrollTo(0, 0);
                  }
              });
          }
      }
  }
  ```

  

## 加速传输策略及其实现与实验

### 加速传输策略

本客户端的RETR实现，是通过从数据连接中每次读取某确定数组大小的数据，然后写入本地的文件中；

STOR实现，是通过从本地的文件中每次读取某确定数组大小的数据，然后写入数据连接中。于是自然想到加大单次传输的数组大小。

```java
               //Stor函数代码片段
				FileOutputStream fileOutput = new FileOutputStream(file);
                DataInputStream dataReader = new DataInputStream(dataSocket.getInputStream());
                writeCmd(new Reply(125,"BINARY Data connection already open; transfer starting.").toString());

                byte[] bytes = new byte[1024*10];
                int len = 0;
                while((len = dataReader.read(bytes, 0 ,bytes.length)) >= 0) {
                    fileOutput.write(bytes, 0 ,len);
                    fileOutput.flush();
                }
                fileOutput.close();
                dataReader.close();
                writeCmd(new Reply(226).toString());
                FTPLogger.writeLog("Data connection closed. " + clientStr, FTPLogger.INFO);
```



### 加速传输实验方式

本组的服务器内含有日志logger，可以打印各个事件发生的时间。

约定RETR或STOR命令后，双方数据连接开始传输数据的时间，与传输完毕关闭连接的时间为所需时间。

通过观察日志显示的，开始传输和结束传输的时间，得到文件传输所需时间。

我们通过改变传输buffer数组的大小，观察传输所需的时间，可以得到buffer为多少的时候传输时间更优



### 加速传输实验结果

表中数值为所需时间，单位为秒

传输内容：512M的big0000文件

| buffer大小（bytes） | ASCII | BINARY |
| ------------------- | ----- | ------ |
| 1024                | 55    | 47     |
| 10240               | 52    | 7      |
| 102400              | 48    | 2      |

由此可见，使用此加速传输策略，在binary模式之下，加速效果尤其明显，从1024时的47秒缩短到2秒

但与之相对的缺点是，需要付出较大的内存作为代价。



## 分数分配

两人各100%
