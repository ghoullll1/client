package com.example.client.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.client.entity.ConfigData;
import com.example.client.entity.LoginData;
import com.example.client.entity.User;
import com.example.client.service.NettyClientService;
import com.example.client.utils.ConfigUtil;
import com.example.client.utils.NettyClientHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final LoginData loginData;
    @Autowired
    NettyClientService nettyClientService;

    @Autowired
    public UserController(LoginData loginData) {
        this.loginData = loginData;
    }

    //登录业务
    @RequestMapping("/login")
    public JSONObject login(User user) throws InterruptedException, IOException {
        if(loginData.getUserID()==""&&!loginData.isLogin()&&!user.isUserEmpty()) {
            loginData.setLogin(true);
            Map<String, Object> map = new HashMap<>();
            map.put("username", user.getAccount());//lzy
            map.put("password", user.getPassword());//123456
            map.put("method", "login");
            map.put("model_type", "");
            ObjectMapper objectMapper = new ObjectMapper();
            // 设置序列化选项
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);  // 设置缩进格式化输出
            objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false); // 不序列化 null 值
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // 不将日期序列化为时间戳
            String loginInfo = null;
            try {
                loginInfo = objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            try {
                nettyClientService.init();
                nettyClientService.sendRequest(loginInfo);
            } catch (InterruptedException e) {
                nettyClientService.getNettyClientHandler().close();
                throw new RuntimeException(e);
            }
            NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
//            try {
                nettyClientHandler.waitForResponse();
//            } catch (InterruptedException e) {
//                nettyClientService.getNettyClientHandler().close();
//                throw new RuntimeException(e);
//            }
            String response = nettyClientHandler.getResponse();
            System.out.println("[login response]: " + response);
            JSONObject jsonResponse = JSONObject.parseObject(nettyClientHandler.getResponse());
            loginData.setLoginRes(jsonResponse);
            int code  = jsonResponse.getInteger("code");
            if (code == 200) {
                JSONObject data=jsonResponse.getJSONObject("data");
                String userId =data.getString("user_id");
                loginData.setUserID(userId);
                JSONObject userInfo=new JSONObject();
                userInfo.put("userId",userId);
                userInfo.put("account",user.getAccount());
                JSONArray projects=new JSONArray();
                JSONObject modelInfoList = new JSONObject();
                data.getJSONArray("projects").forEach((o)->{
                    JSONObject projectInfo=new JSONObject();
                    JSONObject project=(JSONObject)o;
                    projectInfo.put("project_id",project.getString("project_id"));
                    projectInfo.put("project_name",project.getString("project_name"));
                    projectInfo.put("access",project.getJSONArray("access"));
                    projects.add(projectInfo);

                    JSONObject modelInfo=project.getJSONObject("modelInfo");
                    for (String modelType : modelInfo.keySet()) {
                        JSONArray modelArray = modelInfo.getJSONArray(modelType);

                        // 如果在 modelInfoList 中不存在该模型类型，则创建一个空数组
                        if (!modelInfoList.containsKey(modelType)) {
                            modelInfoList.put(modelType, new JSONArray());
                        }

                        // 将当前项目的 modelInfo 数组合并到 modelInfoList 中对应的模型类型中
                        modelInfoList.getJSONArray(modelType).addAll(modelArray);
                    }
                });
                userInfo.put("projectList",projects);
                JSONObject loginRes=new JSONObject();
                loginRes.put("code",code);
                loginRes.put("userInfo",userInfo);
                loginRes.put("modelInfoList",modelInfoList);
                System.out.println(loginRes);
//                ConfigData config = ConfigUtil.getConfigDataByUserId(userId);
//                jsonResponse.put("config",config);
                return loginRes;
            }else if (code == 400001) {
                nettyClientService.getNettyClientHandler().close();
                loginData.setLogin(false);
                return jsonResponse;
            }else{
                return jsonResponse;
            }
        }else{
            return new JSONObject();
        }
    }

    @RequestMapping("/logout")
    public void logout(){
        if(loginData.isLogin()){
            nettyClientService.getNettyClientHandler().close();
            loginData.setUserID("");
            loginData.setLoginRes(null);
            loginData.setLogin(false);
        }
    }
}
