package com.example.ftp

import android.os.Environment
import android.widget.Toast
import java.io.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.Throws

class Client23 {
    private var serverAddress: String? = null
    private var myPort = 1088
    private var transferPort = 1087
    private var controlSocket: Socket? = null
    private var controlreader: BufferedReader? = null
    private var controlwriter: PrintWriter? = null
    private var transferSocket: Socket? = null
    private var transferSocketPassive: ServerSocket? = null   /*传输Socket被动模式*/
    private var transferReader: BufferedReader? = null    /*传输连接读入*/
    private var transferWriter: PrintWriter? = null     /*传输连接输出*/
    private var transferout: OutputStream? = null
    private var transferin: InputStream? = null
    private var state: UserStates = UserStates.OUT    /*用户状态，根据服务器反馈调整*/
    private var type: TransferType = TransferType.BINARY  /*传输类型，根据服务器反馈调整*/
    private var mode: TransferMode = TransferMode.STREAM  /*传输模式，根据服务器反馈调整*/
    private var structure: TransferStructure = TransferStructure.FILE /*传输结构，根据服务器反馈调整*/
    private var response : String = ""
    private lateinit var responseDiv : Array<String>
    var haslogin = false
    var hastransfer = false
    var hasAnonymous = false
    var connectMode = ConMode.PASV
    private var rootAddress = ""
    private var currentResponse: String = ""
    private var responseParameter: String? = null
    private var result = ""

    enum class ConMode{
        PORT, PASV
    }

    private enum class UserStates {            /*用户状态定义*/
        OUT, IN
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

    @Throws(IOException::class)
    fun tryLogin(username: String, password: String): Boolean {
        var success = false
        askAndAnswer("USER $username")
        when(currentResponse){
            "331" -> {
                askAndAnswer("PASS $password")
                if (currentResponse == "230") {
                    state = UserStates.IN
                    success = true
                    init()
                    if(username.lowercase(Locale.getDefault()) == "anonymous") {
                        hasAnonymous = true;
                    }
                }
            }
            "230" -> {
                askAndAnswer("PASS $password")
                if (currentResponse == "230") {
                    state = UserStates.IN
                    success = true
                    init()
                    hasAnonymous = true;
                }
            }
            else -> success = false
        }
        return success
    }

    @Throws(IOException::class)
    fun stor(command: String, args:String?) :String{   // 上传到服务器
        askAndAnswer(command)
        val pathName: String = args!!
        try {
            val file: File = File("$rootAddress/$pathName")
            if (!file.exists()) { //不存在
                return "No such file exists!"
            } else if (file.isFile) { //存在而且是文件
                if (type == TransferType.BINARY) { //二进制传输
                    val fileInput = FileInputStream(file)
                    val dataWriter = DataOutputStream(transferSocket!!.getOutputStream()) /*字节数据输出流*/
                    val bytes = ByteArray(10240)
                    var len = 0
                    while (true) {
                        len = fileInput.read(bytes, 0, bytes.size)
                        if (len == -1) {
                            break;
                        }
                        dataWriter.write(bytes, 0, len);
                        dataWriter.flush();
                    }
                    dataWriter.close()
                    fileInput.close()
                    answer()
                    return  response
                } else { //ascii传输
                    val fileInput =
                        BufferedReader(InputStreamReader(FileInputStream(file), "ASCII"))
                    val dataWriter =
                        BufferedWriter(OutputStreamWriter(transferSocket!!.getOutputStream(), "ASCII"))
                    val chars = CharArray(10240)
                    var len = 0
                    while (fileInput.read(chars, 0, chars.size).also { len = it } >= 0) {
                        dataWriter.write(chars, 0, len)
                        dataWriter.flush()
                    }
                    dataWriter.close()
                    fileInput.close()
                    answer()
                }
            } else {
                return response
            }
        } catch (e: Exception) {
            return response
        }
        return response
    }

    @Throws(IOException::class)
    fun retr(command: String, args:String?) : String{    // 从服务器下载
        askAndAnswer(command)
        if(currentResponse == "450"){ // 服务器发现没这文件
            return "No such file exists! You can use LIST to check."
        }
        try {
            val pathName: String = args!!
            val file: File = File("$rootAddress/$pathName")
            if (type == TransferType.BINARY) { //二进制传输
                val fileOutput = FileOutputStream(file)
                val dataReader = DataInputStream(transferSocket!!.getInputStream()) /*字节数据输出流*/
                var bytes = ByteArray(10240)
                var len = 0
                while (true) { /*完成传输*/
                    len = dataReader.read(bytes, 0, bytes.size)
                    if (len == -1) {
                        break;
                    }
                    fileOutput.write(bytes, 0, len);
                    fileOutput.flush();
                }
                fileOutput.close()
                dataReader.close()
                answer()
            } else { //ascii传输
                val fileOutput = BufferedWriter(OutputStreamWriter(FileOutputStream(file), "ASCII"))
                val dataReader =
                    BufferedReader(InputStreamReader(transferSocket!!.getInputStream(), "ASCII"))
                val chars = CharArray(10240)
                var line: String?
                var len = 0
                while (dataReader.read(chars, 0, chars.size).also { len = it } >= 0) {
                    fileOutput.write(chars, 0, len)
                    fileOutput.flush()
                }
                dataReader.close()
                fileOutput.close()
                answer()
            }
            return response
        } catch (e: Exception) {
            return response
        }
    }

    @Throws(IOException::class)
    fun ask(command: String?) {
        controlwriter!!.println(command)
        controlwriter!!.flush()
    }

    @Throws(IOException::class)
    fun answer() {
        response = controlreader!!.readLine()
        responseDiv = response.split(" ").toTypedArray()
        currentResponse = responseDiv[0]
        responseParameter = responseDiv[1]
    }

    private fun askAndAnswer(command: String){
        ask(command)
        answer()
    }


    @Throws(IOException::class)
    fun transferReceive(ins: InputStream?, br: BufferedWriter) {
        val bytes = ByteArray(1024)
        var data: String
        while (ins!!.read(bytes)!= -1) {
            data = String(bytes,  StandardCharsets.UTF_8)
            br.write(data)
        }
        br.flush()
    }

    @Throws(IOException::class)
    fun noop(command: String) : String{
        currentResponse = "noop error!"
        askAndAnswer(command)
        return response
    }

    @Throws(IOException::class)
    fun stru(command: String, args:String?) : String{
        currentResponse = "stru error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "F" -> structure = TransferStructure.FILE
            "R" -> structure = TransferStructure.RECORD
            "P" -> structure = TransferStructure.PAGE
            else -> return "Wrong parameter! Please check!"
        }
        return response
    }

    @Throws(IOException::class)
    fun mode(command: String, args:String?) : String{
        currentResponse = "mode error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "S" -> mode = TransferMode.STREAM
            "B" -> mode = TransferMode.BLOCK
            "C" -> mode = TransferMode.COMPRESSED
            else -> return "Wrong parameter! Please check!"
        }
        return response
    }

    @Throws(IOException::class)
    fun type(command: String, args:String?) : String{
        currentResponse = "type error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "A" -> type = TransferType.ASCII
            "I" -> type = TransferType.BINARY
            else -> return "Wrong parameter! Please check!"
        }
        return response
    }

    @Throws(IOException::class)
    fun pasv(command: String) : String{
        if(hasAnonymous) {
            return "Permission denied to anonymous! "
        }
        currentResponse = "pasv inner error!"
        askAndAnswer(command)
        when (currentResponse) {
            "227" -> {
                val stringSplit = (responseDiv[1])!!.split(",".toRegex()).toTypedArray()
                val hostName =
                    stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3]
                val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
                try {
                    transferSocket = Socket(hostName, port)
                    val outputStream = transferSocket!!.getOutputStream()
                    transferWriter = PrintWriter(OutputStreamWriter(outputStream))
                    val inputStream = transferSocket!!.getInputStream()
                    transferReader = BufferedReader(InputStreamReader(inputStream))
                } catch (e: Exception) {
                    return "Can't connect in passive mode!"
                }
            }
            "530" -> return  "not log in!"
            "532" -> return  "permission denied to anonymous!!"
            else -> return "pasv number Error!"
        }
        return response
    }

    @Throws(IOException::class)
    fun port(command: String, args: String?) : String{
        currentResponse = "port inner error!"
        if(args == null || args == "") {
            return "Lack of parameter! "
        }
        val stringSplit = args!!.split(",".toRegex()).toTypedArray()
        if(stringSplit.size < 6){
            return "Lack of parameter! "
        }
        if(hasAnonymous) {
            return "Permission denied to anonymous! "
        }
        val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
        transferPort = port
        transferSocketPassive = ServerSocket(transferPort)
        askAndAnswer(command)
        try{
            transferSocket = transferSocketPassive!!.accept()
            when(currentResponse){
                "200" -> {
                    try {
                        val outputStream = transferSocket!!.getOutputStream()
                        transferWriter = PrintWriter(OutputStreamWriter(outputStream))
                        val inputStream = transferSocket!!.getInputStream()
                        transferReader = BufferedReader(InputStreamReader(inputStream))
                    } catch (e: IOException) {
                        return "Can't connect in port !"
                    }
                }
                "530" -> return  "530 not log in!"
                "532" -> return  "permission denied to anonymous!"
                "501" -> return "501 Wrong parameter!"
                else -> return "port error"
            }
        }catch (e:Exception){
            return response
        }
        return response
    }

    @Throws(IOException::class)
    fun list(command: String) : String{
        currentResponse = "list inner error!"
        askAndAnswer(command)
        return response
    }

    @Throws(IOException::class)
    fun quit(command: String) : String{
        currentResponse = "quit error!"
        askAndAnswer(command)
        when (currentResponse) {
            "221" -> {
                transferDisconnect()
                controlSocket!!.close()
                controlwriter!!.close()
                controlreader!!.close()
            }
        }
        return response
    }


    // 关闭数据连接
    @Throws(IOException::class)
    fun transferDisconnect() {
        transferout!!.close()
        transferin!!.close()
        transferSocket!!.close()
        hastransfer = false
    }

    // QUIT
    @Throws(IOException::class)
    fun closeConnection() {
        ask("QUIT")
        controlreader!!.close()
        controlwriter!!.close()
        controlSocket!!.close()
        haslogin = false
        state = UserStates.OUT
    }

    private fun init() {
        rootAddress = Environment.getExternalStorageDirectory().path + "/Client"
        val rootDir: File = File("$rootAddress")
        if(!rootDir.exists()) {
            rootDir.mkdir()
        }
    }
}