package com.example.ftp.tools

import android.widget.Toast
import com.example.ftp.Client23
import java.io.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.Throws


object InnerOperation {
    private var myClient: Client23? = null
    private var serverAddress: String? = null
    private const val port = 1088
    private var username: String? = ""
    private var password: String? = ""
    var success = false

    @JvmStatic
    fun createClient(){
        if (myClient == null) {
            myClient = Client23()
        }
        if (myClient!!.haslogin) {
            try {
                myClient!!.closeConnection()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun passParameters(newAddress: String?, newUser: String?, newPassWord: String?) {
        serverAddress = newAddress
        username = newUser
        password = newPassWord
        createClient()
    }

    @JvmStatic
    fun connect(passUser: String?): Boolean {
        success = true
        Thread {
            try {
                if (myClient!!.haslogin) {
                    myClient!!.closeConnection()
                }
                when (passUser) {
                    "anonymous" -> if (myClient!!.tryConnect(serverAddress, port)) {
                        success = true
                    }
                    username -> if (myClient!!.tryConnect(serverAddress, port)) {
                        if (myClient!!.tryLogin(username!!, password!!)) {
                            success = true
                        }
                    }
                }
            } catch (e: IOException) {
                success = false
            }
        }.start()
        return success
    }

    @JvmStatic
    @Throws(IOException::class)
    fun disconnect(): Boolean {
        success = true
        Thread {
            try {
                myClient!!.closeConnection()
                myClient!!.transferDisconnect()
            } catch (e: IOException) {
                success = false
            }
        }.start()
        return success
    }

    fun upload(remotePath: String?, localPath: String?): Boolean {
        success = true
        Thread {
            success = try {
                if (remotePath != null) {
                    myClient!!.upload(remotePath, localPath)
                }
                true
            } catch (e: IOException) {
                false
            }
        }.start()
        return success
    }

    fun download(remotePath: String?, localPath: String?, filename: String?): Boolean {
        success = true
        Thread {
            success = try {
                myClient!!.download(remotePath!!, localPath!!, filename!!)
                true
            } catch (e: IOException) {
                false
            }
        }.start()
        return success
    }

    fun dummyPacket(command: String): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.noop(command)
            }catch (e:IOException){
                "NOOP error!"
            }
        }.start()
        return result
    }

    fun setTransferStructure(command: String, args: String?): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.stru(command,args)
            }catch (e:IOException){
                "STRU error!"
            }
        }.start()
        return result
    }

    fun setTransferMode(command: String, args: String?): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.mode(command, args)
            }catch (e:IOException){
                "MODE error!"
            }
        }.start()
        return result
    }

    fun setTransferType(command: String, args: String?): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.type(command, args)
            }catch (e:IOException){
                "TYPE error!"
            }
        }.start()
        return result
    }

    fun specifyAddressAndPort(command: String, args: String?): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.port(command, args)
            }catch (e:IOException){
                "PORT error!"
            }
        }.start()
        return result
    }

    fun enterPassive(command: String): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.pasv(command)
            }catch (e:IOException){
                "PASV error!"
            }
        }.start()
        return result
    }

    fun disconnect(command: String): String {
        var result = ""
        Thread {
            result = try {
                myClient!!.quit(command)
            }catch (e:IOException){
                "QUIT error!"
            }
        }.start()
        return result
    }

}