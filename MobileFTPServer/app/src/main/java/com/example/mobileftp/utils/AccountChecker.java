package com.example.mobileftp.utils;

import java.util.ArrayList;

public class AccountChecker {
    private ArrayList<String> nameList= null;
    private ArrayList<String> passList= null;

    public AccountChecker() {
        nameList= new ArrayList<>();
        passList= new ArrayList<>();
        nameList.add("admin");
        passList.add("123456");
    }

    public boolean checkUser(String username) {
        return nameList.contains(username);
    }
    public boolean checkPass(String username, String password) {
        return (checkUser(username) && (nameList.indexOf(username)==password.indexOf(password)));
    }
}
