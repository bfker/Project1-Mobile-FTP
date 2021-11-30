package com.example.mobileftp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobileftp.utils.LogRefresher;
import com.example.mobileftp.utils.StartBtnListener;

public class MainActivity extends AppCompatActivity {
    StartBtnListener startBtnListener;
    FTPServer ftpServer;
    LogRefresher logRefresher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initContent();
        getPermission();

    }

    private void initContent() {
        ftpServer = new FTPServer();
        Button startButton = (Button)findViewById(R.id.startBtn);
        startButton.setOnClickListener(new StartBtnListener(startButton, ftpServer));
        //startBtnListener = new StartBtnListener((Button)findViewById(R.id.startBtn), ftpServer);
        logRefresher = new LogRefresher((TextView)findViewById(R.id.logView));
    }

    private void getPermission() {
        if(ContextCompat.checkSelfPermission
                (this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions
                    (this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);
        }
    }

}