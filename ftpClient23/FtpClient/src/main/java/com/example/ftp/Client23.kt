package com.example.ftp

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
    private var type: TransferType = TransferType.ASCII  /*传输类型，根据服务器反馈调整*/
    private var mode: TransferMode = TransferMode.STREAM  /*传输模式，根据服务器反馈调整*/
    private var structure: TransferStructure = TransferStructure.FILE /*传输结构，根据服务器反馈调整*/
    private lateinit var response : String
    private lateinit var responseDiv : Array<String>
    var haslogin = false
    var hastransfer = false
    var connectMode = ConMode.PASV
    private val rootAddress = "./"
    private var depth = 0
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
                }
            }
            else -> success = false
        }
        return success
    }

    @Throws(IOException::class)
    fun upload(remotePath: String, localPath: String?) {   // 上传下载都还没有写过

    }

    @Throws(IOException::class)
    fun download(remotePath: String, filename: String, newlocalPath: String) {    // 上传下载都还没有写过

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
        return currentResponse
    }

    @Throws(IOException::class)
    fun stru(command: String, args:String?) : String{
        currentResponse = "stru error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "F" -> structure = TransferStructure.FILE
            "R" -> structure = TransferStructure.RECORD
            "P" -> structure = TransferStructure.PAGE
        }
        return currentResponse
    }

    @Throws(IOException::class)
    fun mode(command: String, args:String?) : String{
        currentResponse = "mode error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "S" -> mode = TransferMode.STREAM
            "B" -> mode = TransferMode.BLOCK
            "C" -> mode = TransferMode.COMPRESSED
        }
        return currentResponse
    }

    @Throws(IOException::class)
    fun type(command: String, args:String?) : String{
        currentResponse = "type error!"
        askAndAnswer(command)
        when(args!!.uppercase(Locale.getDefault())){
            "A" -> type = TransferType.ASCII
            "I" -> type = TransferType.BINARY
        }
        return currentResponse
    }

    @Throws(IOException::class)
    fun pasv(command: String) : String{
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
            else -> return "pasv number Error!"
        }
        return currentResponse
    }

    @Throws(IOException::class)
    fun port(command: String, args: String?) : String{
        currentResponse = "port inner error!"
        askAndAnswer(command)
        val stringSplit = args!!.split(",".toRegex()).toTypedArray()
        val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
        transferPort = port
        when(currentResponse){
            "200" -> {
                try {
                    transferSocketPassive = ServerSocket(transferPort)
                    transferSocket = transferSocketPassive!!.accept()
                    val outputStream = transferSocket!!.getOutputStream()
                    transferWriter = PrintWriter(OutputStreamWriter(outputStream))
                    val inputStream = transferSocket!!.getInputStream()
                    transferReader = BufferedReader(InputStreamReader(inputStream))
                } catch (e: IOException) {
                    return "Can't connect in port !"
                }
            }
            "530" -> return  "530 not log in!"
            "501" -> return "501 Wrong parameter!"
            else -> return "port error"
        }
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
        return currentResponse
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
}