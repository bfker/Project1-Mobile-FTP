package com.example.mobileftp.utils;

import java.nio.charset.StandardCharsets;

public class Reply {
    private int stCode;
    private String description;

    public Reply(int stCode) {
        this.stCode = stCode;
        switch (stCode){

            case 125:
                description = "Data connection already open; transfer starting.";
                break;
            case 150:
                description = "File status okay; about to open data connection.";
                break;

            case 200:
                description = "OK.";
                break;

            case 221:
                description = "Service closing control connection.";
                break;

            case 226:
                description = "File received ok. Closing data connection.";
                break;

            case 227:
                description = "Entering Passive Mode.";
                break;

            case 230:
                description = "User logged in.";
                break;

            case 250:
                description = "Requested file action okay, completed.";
                break;

            case 331:
                description = "User name okay, need password.";
                break;

            case 332:
                description = "Need account for login.";
                break;

            case 425:
                description = "Can't open data connection.";
                break;

            case 450:
                description = "Requested file action not taken.";
                break;

            case 451:
                description = " Requested action aborted. Local error in processing.";
                break;

            case 500:
                description = "Syntax error.";
                break;

            case 501:
                description = "Syntax error in parameters or arguments.";
                break;

            case 502:
                description = "Command not implemented.";
                break;

            case 530:
                description = "Not logged in.";
                break;

            case 532:
                description = " Need account for storing files.";
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
