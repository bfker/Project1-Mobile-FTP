package com.example.mobileftp.utils;

import java.util.ArrayList;
import java.util.Locale;

public class AccountChecker {
    private ArrayList<String> nameList= null;
    private ArrayList<String> passList= null;

    public AccountChecker() {
        nameList= new ArrayList<>();
        passList= new ArrayList<>();
        nameList.add("admin");
        passList.add("123456");

        nameList.add("test");
        passList.add("test");
    }

    public boolean checkUser(String username) {
        return nameList.contains(username);
    }
    public boolean checkPass(String username, String password) {
        return (checkUser(username) && (nameList.indexOf(username)==passList.indexOf(password)));
    }

    public static boolean isAnonymous(String username) {
        return username.toLowerCase(Locale.ROOT).equals("anonymous");
    }
}
