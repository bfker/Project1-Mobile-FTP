package com.example.mobileftp.utils;

import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.util.logging.Logger;

public class LogRefresher {
    private static TextView textView = null;
    public LogRefresher(TextView tv) {
        textView = tv;
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    public static void refresh(String newLog) {
        if(textView != null) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.append(newLog);
                    int scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();
                    if (scrollAmount > 0)
                        textView.scrollTo(0, scrollAmount);
                    else
                        textView.scrollTo(0, 0);
                }
            });
        }
    }
}
