package com.example.ftp

import android.Manifest
import com.example.ftp.tools.InnerOperation.connect
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.content.Intent
import android.widget.Button
import com.example.ftp.tools.InnerOperation.passParameters
import kotlin.Throws
import java.io.IOException

class HomepageActivity : AppCompatActivity() {
    private var serverAddress: String? = ""  // 服务器的IP地址
    private var username: String? = null    // 用户名
    private var password: String? = null //密码
    private var controlPort = 1088         // 控制连接端口
    private var inputAddress: EditText? = null
    private var inputUser: EditText? = null
    private var inputPassword: EditText? = null

    private fun toastShow(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }

    private fun bindInputs(){
        inputAddress = findViewById(R.id.inputCommand)
        inputUser = findViewById(R.id.inputUser)
        inputPassword = findViewById(R.id.inputPassword)
    }

    private fun writeDown(){
        val storageMaintainer = getSharedPreferences("connectData", MODE_PRIVATE).edit()
        storageMaintainer.putString("address", serverAddress)
        storageMaintainer.putString("username", username)
        storageMaintainer.putString("password", password)
        storageMaintainer.apply()
    }

    private fun collectData(){
        serverAddress = inputAddress!!.text.toString()
        username = inputUser!!.text.toString()
        password = inputPassword!!.text.toString()
    }

    private fun askAgreement(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitiy_homepage)
        askAgreement()
        bindInputs()
        val connectButton = findViewById<Button>(R.id.commandButton)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val myStorage = getSharedPreferences("connectData", MODE_PRIVATE)
        serverAddress = myStorage.getString("address", "10.72.60.31")
        username = myStorage.getString("username", "admin")
        password = myStorage.getString("password", "12345")
        connectButton.setOnClickListener {
            try {
                if (connect()) {
                    toastShow("Successfully connected!")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else{
                    toastShow("Can't connect!")
                }
            } catch (e: IOException) {
                toastShow("Error! Can't connect!")
            }
        }
        loginButton.setOnClickListener {
            if (login()) {
                toastShow("Successfully log in!")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    @Throws(IOException::class)
    fun connect(): Boolean {
        val myUser = "anonymous"
        serverAddress = inputAddress!!.text.toString()
        if (serverAddress == "") {
            toastShow("Please write the IP address!")
            return false
        }
        passParameters(serverAddress, username, password)
        if (!connect(myUser)) {
            toastShow("Wrong IP address!")
            return false
        }
        return true
    }

    private fun login(): Boolean {
        collectData()
        val myUser = username
        if (serverAddress == "" || username == "" || password == "") {
            toastShow("Please fill the blank above!")
            return false
        }
        passParameters(serverAddress,username,password)
        if (!connect(myUser)) {
            toastShow("Error! Can't log in!")
            return false
        }
        writeDown()
        return true
    }
}