package com.example.client.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class LoginData {
    private JSONObject loginRes;
    private String userID="";
    private boolean isLogin=false;
}
