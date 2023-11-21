package com.example.client.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.client.entity.ConfigData;
import com.example.client.entity.Content;
import com.example.client.entity.UploadData;
import com.example.client.entity.User;
import com.example.client.service.FileChangeService;
import com.example.client.service.MessageProducer;
import com.example.client.service.NettyClientService;
import com.example.client.utils.ConfigUtil;
import com.example.client.utils.ExcelUtil;
import com.example.client.utils.NettyClientHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    NettyClientService  nettyClientService;

    @Autowired
    FileChangeService  fileChangeService;

    private String userId="";
    private boolean isLogin=false;

    private JSONObject loginRes;

    @RequestMapping("/test")
    public void test(String s) throws IOException {
        JSONObject response = JSONObject.parseObject(s);
        System.out.println(response);
        String modelType = response.getJSONObject("data").getString("modelType");
        if (modelType.equals("Excel")) {
            String modelId = response.getJSONObject("data").getString("modelId");
            JSONArray modefiedList = response.getJSONObject("data").getJSONArray("modefiedList");
            for (int i = 0; i < modefiedList.size(); i++) {
                JSONObject modefied = modefiedList.getJSONObject(i);
                String sheetName = modefied.getString("sheetName");
                String value = modefied.getString("value");
                String col = modefied.getString("col");
                int row = Integer.parseInt(modefied.getString("row"));
                int isSuccess = ExcelUtil.updateExcel(modelId, sheetName, row, col, value);
                System.out.println(isSuccess);
                if (isSuccess==1) {
                    System.out.println("修改成功");
                    Map<String,String> map=new HashMap<>();
                    map.put("code", "200");
                    map.put("method", "responseModifyModeData");
                    map.put("modelId", modelId);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String s1 = objectMapper.writeValueAsString(map);
                    System.out.println(s1);
//                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
                }else if(isSuccess==-1){
                    System.out.println("文件被打开");
                    Map<String,String> map=new HashMap<>();
                    map.put("code", "210");
                    map.put("method", "responseModifyModeData");
                    map.put("modelId", modelId);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String s1 = objectMapper.writeValueAsString(map);
                    System.out.println(s1);
//                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));

                }else if(isSuccess==0){
                    System.out.println("修改失败");;
                    Map<String,String> map=new HashMap<>();
                    map.put("code", "211");
                    map.put("method", "responseModifyModeData");
                    map.put("modelId", modelId);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String s1 = objectMapper.writeValueAsString(map);
                    System.out.println(s1);
//                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
                }
            }
        }
    }

    //上传覆盖文件
    @RequestMapping("/upload")
    public int upload(String filePath,String cover,String description) throws IOException, InterruptedException {
        File file = new File(filePath);
        ExcelUtil excelUtil = new ExcelUtil();
        excelUtil.setFile(file);
        Content c = ConfigUtil.getContentByFilePath(filePath);
        JSONObject data = loginRes.getJSONObject("data");
        JSONArray projects=data.getJSONArray("projects");
        String project_id="";
        UploadData uploadData = new UploadData();
        for (Object o : projects) {
            JSONObject json = (JSONObject) o;
            if(c.getProjectName().equals(json.getString("project_name"))){
                project_id=json.getString("project_id");
            }
        }
        uploadData.setModel_id(c.getModelId());
        uploadData.setUser_id(userId);
        uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        uploadData.setFile_name(userId+"_Excel_"+sdf.format(new Date())+".json");//包含后缀的文件名
        uploadData.setModel_name(c.getFileName());//不包含后缀
        if(cover.equals("0")){
            uploadData.setCover(true);
        }else{
            uploadData.setCover(false);
        }
        uploadData.setModel_name(file.getName().substring(0, file.getName().lastIndexOf('.')));
        uploadData.setProject_id(project_id);
        uploadData.setDescription(description);
        uploadData.setModel_type("Excel");

        JSONObject jsonObj = excelUtil.toJsonObj();
        uploadData.setModel_data(jsonObj);
        String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        System.out.println(jsonObject);
        //将文件信息及数据发送到rabbitmq中
        messageProducer.sendMessage(jsonObject);

        //等待netty的response,并根据返回的code处理
        NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
        nettyClientHandler.sendMessage();
        try {
            nettyClientHandler.waitForResponse();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String response = nettyClientHandler.getResponse();
        System.out.println("[upload response]: " + response);
        JSONObject jsonResponse = JSONObject.parseObject(response);
        Integer code = jsonResponse.getInteger("code");
        if(code==210){
            fileChangeService.updateUploadDate(Path.of(filePath));//更新文件上传时间
            fileChangeService.updateConfig(Path.of(filePath));//更新文件修改时间
            fileChangeService.updateDescription(Path.of(filePath),description);
            System.out.println("上传成功");
            return code;
        } else if (code==211) {
            System.out.println("上传失败");
            return code;
        }else if(code==500401){
            System.out.println("上传失败：该项目中模型已存在");
            return code;
        }else{
            return code;
        }
    }

    //上传新文件
    @RequestMapping("/uploadNew")
    public int uploadNew(String projectName, String filePath,String description) throws IOException, InterruptedException, ParseException {
        System.out.println(filePath);
        System.out.println(projectName);

        File file = new File(filePath);
        String fileName=file.getName().substring(0, file.getName().lastIndexOf("."));
        String fileType=file.getName().substring(file.getName().lastIndexOf(".")+1);
        ExcelUtil excelUtil = new ExcelUtil();
        excelUtil.setFile(file);

        JSONObject data = loginRes.getJSONObject("data");
        JSONArray projects = data.getJSONArray("projects");
        String project_id="";
        UploadData uploadData = new UploadData();
        for (Object obj : projects) {
            if (obj instanceof JSONObject) {
                JSONObject json = (JSONObject) obj;
                if(projectName.equals(json.getString("project_name"))){
                    project_id=json.getString("project_id");
                }
            }
        }
        uploadData.setUser_id(userId);
        uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
        uploadData.setModel_type("Excel");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        uploadData.setFile_name(userId+"_Excel_"+sdf1.format(new Date())+".json");//包含后缀的文件名
        uploadData.setModel_name(fileName);//不包含后缀
        uploadData.setCover(true);//改为true和false,有modelId时,true为覆盖,false创建新版本,没有modelId时,都是添加新模型
        uploadData.setModel_name(file.getName().substring(0, file.getName().lastIndexOf('.')));
        uploadData.setProject_id(project_id);
        JSONObject jsonObj = excelUtil.toJsonObj();
        uploadData.setModel_data(jsonObj);
        uploadData.setDescription(description);
        String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        System.out.println(jsonObject);
        //将文件信息及数据发送到rabbitmq中
        messageProducer.sendMessage(jsonObject);

        //等待netty的response,并根据返回的code处理
        NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
        nettyClientHandler.sendMessage();
        try {
            nettyClientHandler.waitForResponse();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String response = nettyClientHandler.getResponse();
        System.out.println("[upload response]: " + response);
        JSONObject jsonResponse = JSONObject.parseObject(response);
        Integer code = jsonResponse.getInteger("code");
        if(code==210){
            Content content = new Content();
            content.setFilePath(filePath);
            content.setDescription(description);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
            Date modifiedDate = fileChangeService.getModifiedDate(Path.of(filePath));
            content.setModifiedDate(sdf.format(modifiedDate));

            content.setUploadDate(sdf.format(new Date()));
            content.setFileType(fileType);
            content.setFileName(fileName);
            content.setUserId(userId);
            content.setProjectName(projectName);

            fileChangeService.updateUploadDate(Path.of(filePath));
            JSONArray dataArray= jsonResponse.getJSONArray("data");
            for (Object o : dataArray) {
                JSONObject j = (JSONObject) o;
                String modelId = j.getString("modelId");
                content.setModelId(modelId);
                ConfigUtil.addContent(content);
                fileChangeService.updateModelId(Path.of(filePath), modelId);
            }
            fileChangeService.init(userId);
            System.out.println("上传成功");
            return code;
        } else if (code==211) {
            System.out.println("上传失败");
            return code;
        }else if(code==500401){
            System.out.println("上传失败：该项目中模型已存在");
            return code;
        }else{
            return code;
        }
    }

    @RequestMapping("/logout")
    public void logout(){
        if(isLogin){
            nettyClientService.getNettyClientHandler().close();
            userId="";
            isLogin=false;
        }
    }

    @RequestMapping("/removeNotify")
    public void removeNotify(String filePath){
        fileChangeService.removeNotify(filePath);
    }

    //登录业务
    @PostMapping("/login")
    public JSONObject login(User user) throws InterruptedException, IOException {
        System.out.println(user);

        if(userId==""&&!isLogin&&!user.isUserEmpty()) {
            isLogin=true;
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
            String json = null;
            try {
                json = objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            try {
                nettyClientService.init();
                nettyClientService.sendRequest(json);
            } catch (InterruptedException e) {
                nettyClientService.getNettyClientHandler().close();
                throw new RuntimeException(e);
            }
            NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
            try {
                nettyClientHandler.waitForResponse();
            } catch (InterruptedException e) {
                nettyClientService.getNettyClientHandler().close();
                throw new RuntimeException(e);
            }
            String response = nettyClientHandler.getResponse();
            System.out.println("[login response]: " + response);
            JSONObject jsonResponse = JSONObject.parseObject(nettyClientHandler.getResponse());
            loginRes = jsonResponse;
            int code = jsonResponse.getInteger("code");
            if (code == 200) {
                userId =jsonResponse.getJSONObject("data").getString("user_id");
                ConfigData config = ConfigUtil.getConfigDataByUserId(userId);
                jsonResponse.put("config",config);
                System.out.println(jsonResponse);
                System.out.println(userId);
                try {
                    fileChangeService.init(userId);
                } catch (IOException | ParseException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (code == 400001) {
                nettyClientService.getNettyClientHandler().close();
                isLogin=false;
            }
            return jsonResponse;
        }else{
            return new JSONObject();
        }
    }

}
