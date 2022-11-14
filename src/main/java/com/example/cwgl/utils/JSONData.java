package com.example.cwgl.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller 返回对象
 * 返回类型 JSONData
 * @author lcc
 * @date 2022/3/18 15:08
 */
public class JSONData extends HashMap<String, Object> {

    public final static int OK = 1;
    public final static int ERROR = -1;
    public final static int FAIL = 0;

    private final static ObjectMapper mapper = new ObjectMapper();

    public JSONData() {
        put("code", OK);
        put("msg", "成功");

    }
    public static JSONData result(Integer code){
        return code==1?ok():fail();
    }
    public static JSONData fail() {
        return error(FAIL, "失败了");
    }

    public static JSONData fail(String msg) {
        return error(FAIL, msg);
    }
    public static JSONData error() {
        return error(ERROR, "系统异常，请联系管理员");
    }

    public static JSONData error(String msg) {
        return error(ERROR, msg);
    }

    public static JSONData error(int code, String msg) {
        JSONData r = new JSONData();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static JSONData ok(Object obj) {
        JSONData r = new JSONData();
        r.put("code", OK);

        if(obj instanceof String){
            r.put("msg", obj);
            return r;
        }
        if(obj instanceof List){
            r.put("msg", "成功");
            r.put("count", ((List) obj).size());
            r.put("data", obj);
            return r;
        }
        return r.put("data",obj);
    }

    public static JSONData ok() {
        return new JSONData();
    }

    @Override
    public JSONData put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public String toJSON() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
