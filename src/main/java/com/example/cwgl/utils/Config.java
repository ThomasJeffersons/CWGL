package com.example.cwgl.utils;


import com.example.cwgl.entity.User;

import javax.servlet.http.HttpSession;

public class Config {

    public static String CURRENT_USERNAME = "currentUser";

    //Result
    public static int SUCCESS=200; //成功
    public static int UNSUCCESS=400;   //失败
    public static int ERROR=500;   //异常

    //启用自定义日志打印
    public static boolean ENABLE_CUSTOMEIZE_LOG = true;


    public static User getSessionUser(HttpSession session){
        return (User)session.getAttribute(CURRENT_USERNAME);
    }


}
