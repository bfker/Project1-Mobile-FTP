package com.example.mobileftp.utils;

import java.nio.charset.StandardCharsets;

public class Reply {
    private int stCode;
    private String description;

    public Reply(int stCode) {
        this.stCode = stCode;
        switch (stCode){
            case 200:
                description = "OK.";
                break;

            case 230:
                description = "User logged in.";
                break;

            case 331:
                description = "User name okay, need password.";
                break;

            case 332:
                description = "Need account for login.";
                break;

            case 500:
                description = "Syntax error.";
                break;

            case 501:
                description = "Syntax error in parameters or arguments.";
                break;

            case 530:
                description = "Not logged in.";
                break;
            case 227:
                description = "Entering Passive Mode.";
                break;
        }
    }

    public Reply(int stCode, String description) {
        this.stCode = stCode;
        this.description = description;
    }

    @Override
    public String toString() {
        return stCode + " "+ description;
    }

    public byte[] toBytes() {
        return (stCode + " "+ description).getBytes(StandardCharsets.UTF_8);
    }

}
