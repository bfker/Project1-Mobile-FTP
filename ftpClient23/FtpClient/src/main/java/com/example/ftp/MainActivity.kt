package com.example.ftp

import android.Manifest
import com.example.ftp.tools.InnerOperation.disconnect
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Bundle
import android.view.Gravity
import com.google.android.material.navigation.NavigationView
import android.widget.Toast
import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.ftp.tools.InnerOperation.dummyPacket
import com.example.ftp.tools.InnerOperation.enterPassive
import com.example.ftp.tools.InnerOperation.setTransferMode
import com.example.ftp.tools.InnerOperation.setTransferStructure
import com.example.ftp.tools.InnerOperation.setTransferType
import com.example.ftp.tools.InnerOperation.specifyAddressAndPort
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var fileList: List<File>? = null
    private var inputCommand: EditText? = null
    private var currentCommand: String = ""
    private var respondBlank : TextView? = null
    private var result : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermissions()
        bind()
        master()
    }

    private fun toastShow(sentence: String) {  /*弹出小提示用的函数*/
        Toast.makeText(
            this,
            sentence,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun bind(){
        inputCommand = findViewById(R.id.inputCommand)
        respondBlank = findViewById(R.id.respondView)
    }

    private fun master(){
        val commandButton = findViewById<Button>(R.id.commandButton)
        commandButton.setOnClickListener(){
            currentCommand = inputCommand!!.text.toString()
            val index = currentCommand.indexOf(' ')
            val commandName =
                if (index == -1) currentCommand.uppercase(Locale.getDefault()) else currentCommand.substring(0, index).uppercase(Locale.getDefault()) /* 截取第一部分 这一部分是命令名*/
            val args = if (index == -1) null else currentCommand.substring(index + 1) /*而后面的则是参数*/
            result = when (commandName) {
                "QUIT" -> disconnect(currentCommand)
                "PASV" -> enterPassive(currentCommand)
                "PORT" -> specifyAddressAndPort(currentCommand, args)
                "TYPE" -> setTransferType(currentCommand, args)
                "MODE" -> setTransferMode(currentCommand, args)
                "STRU" -> setTransferStructure(currentCommand, args)
                "NOOP" -> dummyPacket(currentCommand)
                else -> "[Client] Unknown command. Please check."
            }
            respondBlank?.text = result
        }
    }
    // 以下是请求用户给予权限的部分
    private fun askForPermissions() {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 2
            )
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
            }
        }
}