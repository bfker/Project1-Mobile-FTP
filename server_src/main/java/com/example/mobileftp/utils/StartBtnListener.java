package com.example.mobileftp.utils;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.mobileftp.FTPServer;
import com.example.mobileftp.MainActivity;
import com.example.mobileftp.R;

public class StartBtnListener implements View.OnClickListener {
    private Button startBtn;
    private FTPServer ftpServer;
    public StartBtnListener(Button button, FTPServer server) {
        super();
        this.startBtn = button;
        this.ftpServer = server;
    }
    @Override
    public void onClick(View view) {
        startBtn.setEnabled(false);
        Toast.makeText(view.getContext(),"you click the startBtn",Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ftpServer.run();
            }
        }).start();
    }
}
