package com.example.ftp.tools

import com.example.ftp.Client23
import java.io.*
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

    @JvmStatic
    @Throws(IOException::class)
    fun disconnect(): Boolean {
        success = false
        var t = Thread {
            try {
                myClient!!.closeConnection()
                myClient!!.transferDisconnect()
                success = true
            } catch (e: IOException) {
                success = false
            }
        }
        t.start()
        t.join(5000)
        return success
    }

    fun dummyPacket(command: String): String {
        var result = ""
        var t = Thread {
            result = try {
                myClient!!.noop(command)
            }catch (e:IOException){
                "NOOP error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun setTransferStructure(command: String, args: String?): String {
        var result = ""
        var t = Thread {
            result = try {
                myClient!!.stru(command,args)
            }catch (e:IOException){
                "STRU error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun setTransferMode(command: String, args: String?): String {
        var result = ""
        var t = Thread {
            result = try {
                myClient!!.mode(command, args)
            }catch (e:IOException){
                "MODE error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun setTransferType(command: String, args: String?): String {
        var result = ""
        var t = Thread {
            result = try {
                myClient!!.type(command, args)
            }catch (e:IOException){
                "TYPE error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun specifyAddressAndPort(command: String, args: String?): String {
        var result = ""
        val t = Thread {
            result = try {
                myClient!!.port(command, args)
            }catch (e:IOException){
                "PORT thread error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun downLoad(command: String, args: String?): String {
        var result = ""
        val t = Thread {
            result = try {
                myClient!!.retr(command, args)
            }catch (e:IOException){
                "RETR thread error!"
            }
        }
        t.start()
        t.join()
        return result
    }

    fun upLoad(command: String, args: String?): String {
        var result = ""
        val t = Thread {
            result = try {
                myClient!!.stor(command, args)
            }catch (e:IOException){
                "STOR thread error!"
            }
        }
        t.start()
        t.join()
        return result
    }

    fun askForList(command: String): String {
        var result = ""
        val t = Thread {
            result = try {
                myClient!!.list(command)
            }catch (e:IOException){
                "LIST thread error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun enterPassive(command: String): String {
        var result = ""
        val t = Thread {
            result = try {
                myClient!!.pasv(command)
            }catch (e:IOException){
                "PASV error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

    fun disconnect(command: String): String {
        var result = ""
        var t = Thread {
            result = try {
                myClient!!.quit(command)
            }catch (e:IOException){
                "QUIT error!"
            }
        }
        t.start()
        t.join(5000)
        return result
    }

}