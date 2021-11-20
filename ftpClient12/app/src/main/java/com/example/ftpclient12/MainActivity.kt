package com.example.ftpclient12

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.Socket
import java.util.*
import java.lang.Exception
import java.net.ServerSocket

class MainActivity : AppCompatActivity() {
    private var serverAddress = " "
    private var controlSocket: Socket? = null        /*命令Socket*/
    private var transferSocket: Socket? = null   /*传输Socket*/
    private var transferSocketPassive: ServerSocket? = null   /*传输Socket被动模式*/
    private var transferConnection: Socket? = null
    private var controlPort = 8888                /*命令端口*/
    private var transferPort = 8887             /*传输端口*/
    private var currentCommand = " "    /*当前命令*/
    private var currentResponse = " "    /*当前回应*/
    private var userNow = UserStates.OUT       /*当前用户状态*/
    private val legalUser = "aaa"         /*合法用户名*/
    private val legalPassword = "111"         /*合法密码*/
    private var quitMainLoop = false        /*已退出标志*/
    private var hintMessage = " "
    private var rootAddress = "./"  /*客户端的根目录*/
    private var controlReader: BufferedReader? = null
    private var controlWriter: PrintWriter? = null
    private var transferReader: BufferedReader? = null
    private var transferWriter: PrintWriter? = null
    private var state : UserStates = UserStates.OUT
    private var type : TransferType = TransferType.ASCII
    private var mode : TransferMode = TransferMode.STREAM
    private var structure : TransferStructure = TransferStructure.FILE
    private enum class UserStates {            /*用户状态定义*/
        OUT, ANONYMOUS, LOGIN
    }
    private enum class TransferType {            /*用户状态定义*/
        ASCII, BINARY
    }
    private enum class TransferMode {            /*用户状态定义*/
        STREAM, BLOCK, COMPRESSED
    }
    private enum class TransferStructure {            /*用户状态定义*/
        FILE, RECORD, PAGE
    }
    private fun extractFolder(folder: Array<File>): String {  /*文件夹内的文件名提取*/
        var files = ""
        for (folderFile in folder) {
            files = files + folderFile.name + "$"
        }
        return files
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        master()

    }

    private fun master() {
        connectButton.setOnClickListener() {
            serverAddress = inputAddress.text.toString()
            try {
                controlSocket = Socket(serverAddress, controlPort)
                controlReader =
                    BufferedReader(InputStreamReader(controlSocket!!.getInputStream()))
                controlWriter =
                    PrintWriter(OutputStreamWriter(controlSocket!!.getOutputStream()))
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "[Client] Can't connect! Please enter address again!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Toast.makeText(this, "[Client] Successfully connect! Welcome", Toast.LENGTH_SHORT).show()
        commandButton.setOnClickListener() {
            mainLoop()
        }
    }

    private fun mainLoop() {
        currentCommand = inputCommand.text.toString()
        val index = currentCommand.indexOf(' ')
        val commandName = if (index == -1) currentCommand.uppercase(Locale.getDefault()) else currentCommand.substring(0, index)
            .uppercase(Locale.getDefault()) /* 截取第一部分 */
        val args = if (index == -1) null else currentCommand.substring(index + 1)
        when (commandName) {
            "USER" -> authenticationUsername(currentCommand)
            "PASS" -> authenticationPassword(currentCommand)
            "QUIT" -> disconnect(currentCommand)
            "PASV" -> enterPassive(currentCommand)
            "PORT" -> specifyAddressAndPort(currentCommand, args)
            "TYPE" -> setTransferType(currentCommand, args)
            "MODE" -> setTransferMode(currentCommand, args)
            "STRU" -> setTransferStructure(currentCommand, args)
            "RWTR" -> retrieveFile(currentCommand, args)
            "STOR" -> storeFile(currentCommand, args)
            "NOOP" -> dummyPacket(currentCommand)
            else -> Toast.makeText(this, "[Client] Unknown command", Toast.LENGTH_SHORT).show()
        }
    }



    private fun storeFile(command: String, args: String?) {
        TODO("Not yet implemented")
    }

    private fun retrieveFile(command: String, args: String?) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        if (currentResponse == "200 OK") {
            val inputByte = ByteArray(1024)
            var length = 0
            val dataIn = DataInputStream(
                transferSocket!!.getInputStream()
            ) /*接受服务器的数据输入流*/
            var fileOut = FileOutputStream(File(rootAddress + dataIn.readUTF()))
            Toast.makeText(
                this,
                "[Client] Receiving data.",
                Toast.LENGTH_SHORT
            ).show()
            while (true) { /*完成传输*/
                if (dataIn != null) {
                    length = dataIn.read(inputByte, 0, inputByte.size)
                }
                if (length == -1) {
                    break
                }
                fileOut.write(inputByte, 0, length)
                fileOut.flush()
            }
            println("[Client] Complete!")
            fileOut.close()
            dataIn.close()
            transfer = Socket(serverAddress, dataPort)
        } else if (response == "[Server] Is a Directory!") { /*如果下载的是文件夹，则新建文件夹并作为客户端根目录*/
            folderRetrLoop = true
            rootStr = "./$instPara/"
            val clientRoot = File(rootStr).mkdirs()
            if (clientRoot) {
                println("[Client] New folder has been created!")
            }
            instWriter.println("CWD ./$instPara") /*找到服务器中待下载的文件夹并作为新的根目录*/
            instWriter.flush()
            response = instReader.readLine()
            if (response == "OK") {
                instWriter.println("LIST")
                instWriter.flush()
                response = instReader.readLine()
                val folderList = StringTokenizer(response, "$")
                var folderInList: String
                folderRetr = arrayOfNulls(folderList.countTokens() + 1)
                folderRetr!![r++] = "CWD ../" /*还原服务器工作目录为原根目录*/
                while (folderList.hasMoreTokens()) { /*加载文件夹中待下载的文件目录，生成RETR指令序列*/
                    folderInList = folderList.nextToken()
                    inst = "RETR $folderInList"
                    folderRetr!![r++] = inst
                }
            }
        } else println(response)
    }

    private fun dummyPacket(command: String) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
    }

    private fun setTransferStructure(command: String, args: String?) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        if(currentResponse == "200 OK"){
            if (args!!.uppercase(Locale.getDefault()) == "F") {
                structure = TransferStructure.FILE
            } else if (args!!.uppercase(Locale.getDefault()) == "R") {
                structure = TransferStructure.RECORD
            }else if (args!!.uppercase(Locale.getDefault()) == "P") {
                structure = TransferStructure.PAGE
            }
        }
        Toast.makeText(
            this,
            "[Client] Transfer structure set.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setTransferMode(command: String, args: String?) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        if(currentResponse == "200 OK"){
            if (args!!.uppercase(Locale.getDefault()) == "S") {
                mode = TransferMode.STREAM
            } else if (args!!.uppercase(Locale.getDefault()) == "B") {
                mode = TransferMode.BLOCK
            }else if (args!!.uppercase(Locale.getDefault()) == "C") {
                mode = TransferMode.COMPRESSED
            }
        }
        Toast.makeText(
            this,
            "[Client] Transfer mode set.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setTransferType(command: String, args: String?) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        if(currentResponse == "200 OK"){
            if (args!!.uppercase(Locale.getDefault()) == "A") {
                type = TransferType.ASCII
            } else if (args!!.uppercase(Locale.getDefault()) == "I") {
                type = TransferType.BINARY
            }
        }
        Toast.makeText(
            this,
            "[Client] Transfer type set.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun specifyAddressAndPort(command: String, args: String?) {
        val stringSplit = args!!.split(",".toRegex()).toTypedArray()
        val p = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
        transferPort = p
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        try {
            transferSocketPassive = ServerSocket(transferPort)
            transferSocket = transferSocketPassive!!.accept()
            transferWriter = PrintWriter(transferSocket!!.getOutputStream(), true)
        } catch (e: IOException) {
            Toast.makeText(
                this,
                "[Client] Can't connect!.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun authenticationUsername(command: String) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        Toast.makeText(
            this,
            "[Client] User name sent.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun authenticationPassword(command: String) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        Toast.makeText(
            this,
            "[Client] Password sent.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun disconnect(command: String) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        Toast.makeText(
            this,
            "[Client] Quit! ",
            Toast.LENGTH_SHORT
        ).show()
        controlSocket = null
        transferSocket = null
    }

    private fun enterPassive(command: String) {
        controlWriter?.println(command)
        controlWriter?.flush()
        currentResponse = controlReader?.readLine().toString()
        outputstd.text = (currentResponse)
        Toast.makeText(
            this,
            "[Client] Enter passive mode",
            Toast.LENGTH_SHORT
        ).show()
        val stringSplit = currentResponse!!.split(",".toRegex()).toTypedArray()
        val hostName =
            stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3]
        val port = stringSplit[4].toInt() * 256 + stringSplit[5].toInt()
        try {
            transferSocket = Socket(hostName, port)
            transferReader =
                BufferedReader(InputStreamReader(transferSocket!!.getInputStream()))
            transferWriter =
                PrintWriter(OutputStreamWriter(transferSocket!!.getOutputStream()))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "[Client] Can't connect in passive mode!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



