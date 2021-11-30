package com.example.mobileftp.utils;

import android.widget.Button;
import android.widget.TextView;

import com.example.mobileftp.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FTPLogger {
    //private static String logTxt = "";
    public static final int INFO = 0;
    public static final int ERROR = -1;
    public static final int WARNING = 1;

    private static String getTimeStamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static void writeLog(String msg, int TYPE) {
        switch (TYPE){
            case INFO:
                //logTxt = logTxt +  getTimeStamp()+" [info] "+ msg + "\n";
                LogRefresher.refresh(getTimeStamp()+" [info] "+ msg + "\n");

                break;
            case ERROR:
                //logTxt = logTxt +  getTimeStamp()+" [error] "+ msg + "\n";
                LogRefresher.refresh(getTimeStamp()+" [error] "+ msg + "\n");
                break;
        }
    }
}
