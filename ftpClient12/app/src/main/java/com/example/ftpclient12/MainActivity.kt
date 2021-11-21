package com.example.ftpclient12

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.Socket
import java.util.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode
import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private var serverAddress = " " /*服务器的IP地址*/
    private var myAddress = " "   /*客户端的IP地址*/
    private var controlSocket: Socket? = null        /*命令Socket*/
    private var transferSocket: Socket? = null   /*传输Socket*/
    private var transferSocketPassive: ServerSocket? = null   /*传输Socket被动模式*/
    private var transferConnection: Socket? = null
    private var controlPort = 8000                /*默认命令端口号（客户端和服务端一样）*/
    private var transferPort = 7999             /*默认传输端口（客户端和服务端一样）*/
    private var currentCommand = " "    /*当前命令*/
    private var currentResponse = " "    /*当前回应*/
    private var userNow = UserStates.OUT       /*当前用户状态*/
    private val legalUser = "admin"         /*合法用户名*/
    private val legalPassword = "12345"         /*合法密码*/
    private var quitMainLoop = false        /*已退出标志*/
    private var hintMessage = " "
    private var rootAddress = "./"  /*客户端的根目录*/
    private var controlReader: BufferedReader? = null  /*控制连接读入*/
    private var controlWriter: PrintWriter? = null   /*控制连接输出*/
    private var transferReader: BufferedReader? = null    /*传输连接读入*/
    private var transferWriter: PrintWriter? = null     /*传输连接输出*/
    private var state: UserStates = UserStates.OUT    /*用户状态，根据服务器反馈调整*/
    private var type: TransferType = TransferType.ASCII  /*传输类型，根据服务器反馈调整*/
    private var mode: TransferMode = TransferMode.STREAM  /*传输模式，根据服务器反馈调整*/
    private var structure: TransferStructure = TransferStructure.FILE /*传输结构，根据服务器反馈调整*/

    private enum class UserStates {            /*用户状态定义*/
        OUT, ANONYMOUS, LOGIN
    }

    private enum class TransferType {            /*传输类型定义*/
        ASCII, BINARY
    }

    private enum class TransferMode {            /*传输模式定义*/
        STREAM, BLOCK, COMPRESSED
    }

    private enum class TransferStructure {            /*传输结构定义*/
        FILE, RECORD, PAGE
    }

    private fun extractFromFolder(folder: Array<File>): String {  /*文件夹内的文件名提取*/
        var files = ""
        for (folderFile in folder) {
            files = files + folderFile.name + "^"
        }
        return files
    }

    override fun onCreate(savedInstanceState: Bundle?) {  /*从这里开始运行*/
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectDiskReads().detectDiskWrites().detectNetwork()
            .penaltyLog().build());
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
            .build());
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        master()
    }

    private fun toastShow(sentence: String) {  /*弹出小提示用的函数*/
        Toast.makeText(
            this,
            sentence,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun master() {
        connectButton.setOnClickListener() {   /*当connect按钮被按下后，就尝试用输入框里的IP地址发起连接*/
            serverAddress = inputAddress.text.toString()
            try {
                controlSocket = Socket()
                controlSocket!!.bind(InetSocketAddress(controlPort))
                controlSocket!!.connect(InetSocketAddress(serverAddress, controlPort))
                controlReader =
                    BufferedReader(InputStreamReader(controlSocket!!.getInputStream()))
                controlWriter =
                    PrintWriter(OutputStreamWriter(controlSocket!!.getOutputStream()))
            } catch (e: Exception) {
                toastShow("[Client] Can't connect! Please enter address again!")
            }
        }
        toastShow("[Client] Successfully connect! Welcome.")  /*开发中所以放这里，这些应该都在try里，连上了才运行*/
        commandButton.setOnClickListener() {  /*命令按钮生效，进入主循环*/
            mainLoop()
        }
    }

    private fun mainLoop() {
        currentCommand = inputCommand.text.toString()
        val index = currentCommand.indexOf(' ')
        val commandName =
            if (index == -1) currentCommand.uppercase(Locale.getDefault()) else currentCommand.substring(
                0,
                index
            )
                .uppercase(Locale.getDefault()) /* 截取第一部分 这一部分是命令名*/
        val args = if (index == -1) null else currentCommand.substring(index + 1) /*而后面的则是参数*/
        when (commandName) {
            "USER" -> authenticationUsername(currentCommand)
            "PASS" -> authenticationPassword(currentCommand)
            "QUIT" -> disconnect(currentCommand)
            "PASV" -> enterPassive(currentCommand)
            "PORT" -> specifyAddressAndPort(currentCommand, args)
            "TYPE" -> setTransferType(currentCommand, args)
            "MODE" -> setTransferMode(currentCommand, args)
            "STRU" -> setTransferStructure(currentCommand, args)
            "RETR" -> retrieveFile(currentCommand, args)
            "STOR" -> storeFile(currentCommand, args)
            "NOOP" -> dummyPacket(currentCommand)
            "LIST" -> askForList(currentCommand)
            "CWD" -> changeWorkingDirectory(currentCommand, args)
            else -> toastShow("[Client] Unknown command. Please check.")
        }
    }

    private fun askAndAnswer(command: String) {  /*向服务器命令一次，读取一次回应并显示 是大多数命令都需要的基础操作*/
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
    }

    private fun changeWorkingDirectory(command: String, args: String?) {
        askAndAnswer(command)
        when(currentResponse){
            "250" -> toastShow("[Client] change working directory successfully.")
            else -> toastShow("[Client] Error! Can't change working directory.")
        }
    }

    private fun askForList(command: String) {  /*让服务器列出文件列表*/
        askAndAnswer(command)
        when (currentResponse) {
            "250" -> toastShow("[Client] ask for list successfully.")
            else -> toastShow("[Client] Error! Can't ask for list.")
        }
    }

    private fun read(sdFileName: String){
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                var sdCardDIr: File? = getExternalFilesDir(null)
                var fis: FileInputStream = FileInputStream((sdCardDIr?.canonicalPath) + sdFileName)
                var br: BufferedReader = BufferedReader(InputStreamReader(fis))
                var file = FileOutputStream(sdCardDIr, false);
                var buffer = ByteArray(1024);
                var size = -1;
                while (true) {
                    size = fis.read(buffer)
                    if(size == -1){
                        break
                    }
                    else file.write(buffer, 0, size);
                }
                file.close();
                fis.close();
                toastShow("Successfully read !");
            }
        }catch (e: Exception){
            toastShow("Error! Can't read from SD card.")
        }
    }

    private fun storeFile(command: String, args: String?) {
        TODO("Not yet implemented")
    }


    private fun write(content : String, sdFileName: String){
        try{
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                var sdCardDIr : File? = getExternalFilesDir(null)
                var targetFile = File((sdCardDIr?.canonicalPath) + sdFileName)
                var raf : RandomAccessFile = RandomAccessFile(targetFile, "rw")
                raf.seek(targetFile.length())
                raf.write(content.toByteArray())
                raf.close()
            }
        }catch (e: Exception){
            toastShow("Error! Can't write onto SD card.")
        }
    }

    private fun retrieveFile(command: String, args: String?) {
        askAndAnswer(command)
        when(currentResponse){
            "200" -> TODO("Not yet implemented")
            else -> toastShow("[Client] Error! Can't retrieve files.")
        }
    }

    private fun dummyPacket(command: String) {
        askAndAnswer(command)
        when(currentResponse){
            "200" -> toastShow("[Client] Noop successfully.")
            else -> toastShow("[Client] Error! Can't noop.")
        }
    }

    private fun setTransferStructure(command: String, args: String?) {
        askAndAnswer(command)
        when(currentResponse){
            "200" -> {
                when(args!!.uppercase(Locale.getDefault())){
                    "F" -> structure = TransferStructure.FILE
                    "R" -> structure = TransferStructure.RECORD
                    "P" -> structure = TransferStructure.PAGE
                }
                toastShow("[Client] Transfer structure set.")
            }
            else -> toastShow("[Client] Error! Can't set transfer structure.")
        }
    }

    private fun setTransferMode(command: String, args: String?) {
        askAndAnswer(command)
        when(currentResponse){
            "200" -> {
                when(args!!.uppercase(Locale.getDefault())){
                    "S" -> mode = TransferMode.STREAM
                    "B" -> mode = TransferMode.BLOCK
                    "C" -> mode = TransferMode.COMPRESSED
                }
                toastShow("[Client] Transfer mode set.")
            }
            else -> toastShow("[Client] Error! Can't set transfer mode.")
        }
    }

    private fun setTransferType(command: String, args: String?) {
        askAndAnswer(command)
        when(currentResponse){
            "200" -> {
                when(args!!.uppercase(Locale.getDefault())){
                    "A" -> type = TransferType.ASCII
                    "I" -> type = TransferType.BINARY
                }
                toastShow("[Client] Transfer type set.")
            }
            else -> toastShow("[Client] Error! Can't set transfer type.")
        }
    }

    private fun specifyAddressAndPort(command: String, args: String?) {
        val stringSplit = args!!.split(",".toRegex()).toTypedArray()
        val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
        transferPort = port
        askAndAnswer(command)
        when(currentCommand){
            "200" -> {
            try {
                transferSocketPassive = ServerSocket(transferPort)
                transferSocket = transferSocketPassive!!.accept()
                transferWriter = PrintWriter(transferSocket!!.getOutputStream())
                transferReader = BufferedReader(InputStreamReader(transferSocket!!.getInputStream()))
            } catch (e: IOException) {
                toastShow("[Client] Can't reconnect!")
            }
            }
            else -> toastShow("[Client] Error!")
        }
    }

    private fun authenticationUsername(command: String) {
        askAndAnswer(command)
        when (currentResponse) {
            "230" -> toastShow("[Client] User has already been logged in.")
            "331" -> toastShow("[Client] User name is right. Need password.")
            else -> toastShow("[Client] Error!")
        }
    }

    private fun authenticationPassword(command: String) {
        askAndAnswer(command)
        when (currentResponse) {
            "230" -> toastShow("[Client] User logged in successfully.")
            else -> toastShow("[Client] Error!")
        }
    }

    private fun disconnect(command: String) {
        askAndAnswer(command)
        when (currentResponse) {
            "221" -> {
                toastShow("[Client] Disconnected!")
                controlSocket = null
                transferSocket = null
            }
            else -> toastShow(" [Client] Error! Can't Quit!")
        }
    }

    private fun enterPassive(command: String) {
        askAndAnswer(command)
        val index = currentResponse.indexOf(' ')
        val responseName = if (index == -1) currentCommand else currentCommand.substring(0, index)
        /* 截取第一部分 这一部分是应答数字*/
        val args = if (index == -1) null else currentCommand.substring(index + 1) /*而后面的则是参数*/
        when (responseName) {
            "227" -> {
                toastShow("[Client] Enter passive mode")
                val stringSplit = args!!.split(",".toRegex()).toTypedArray()
                val hostName =
                    stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3]
                val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
                try {
                    transferSocket = Socket()
                    transferSocket!!.bind(InetSocketAddress(transferPort))
                    transferSocket!!.connect(InetSocketAddress(hostName, port))
                    transferReader =
                        BufferedReader(InputStreamReader(transferSocket!!.getInputStream()))
                    transferWriter =
                        PrintWriter(OutputStreamWriter(transferSocket!!.getOutputStream()))
                } catch (e: Exception) {
                    toastShow("[Client] Can't connect in passive mode!")
                }
            }
            else -> toastShow("[Client] Error!")
        }
    }

    // 以下是检查和申请权限的部分
    private val permissionCode = 1
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // 检查本应用是否有了 WRITE_EXTERNAL_STORAGE 权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 系统将弹出一个对话框，询问用户是否授权
            ActivityCompat.requestPermissions(this, permissions, permissionCode)
        }
    }

    // 权限申请的结果  // requestCode：请求码  // permissions: 申请的N个权限  // grantResults: 每个权限是否被授权
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // 如果用户没给我们授权...这意味着有此功能就不能用了
                }
            }
        }
    }
}
