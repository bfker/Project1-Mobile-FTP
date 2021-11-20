package com.example.mobileftp;

import androidx.appcompat.app.AppCompatActivity;

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
    }

    private void initContent() {
        ftpServer = new FTPServer();
        Button startButton = (Button)findViewById(R.id.startBtn);
        startButton.setOnClickListener(new StartBtnListener(startButton, ftpServer));
        //startBtnListener = new StartBtnListener((Button)findViewById(R.id.startBtn), ftpServer);
        logRefresher = new LogRefresher((TextView)findViewById(R.id.logView));
    }

}