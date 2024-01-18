package com.example.client.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.client.entity.Content;
import com.example.client.entity.UploadData;
import com.example.client.utils.ConfigUtil;
import com.example.client.utils.ExcelUtil;
import com.example.client.utils.NettyClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class FileUploadService {
    @Autowired
    MessageProducer messageProducer;
    @Autowired
    NettyClientService nettyClientService;
    @Autowired
    FileChangeService fileChangeService;

    //上传已经上传过的模型文件
    public JSONObject upload(String userId, JSONObject loginRes, String filePath, String cover, String description, String modelType) throws IOException, InterruptedException {
        File file = new File(filePath);

        Content c = ConfigUtil.getContentByFilePath(filePath);
        //filePath下模型存在且模型已上传过
        if (c != null) {
            JSONObject data = loginRes.getJSONObject("data");
            JSONArray projects = data.getJSONArray("projects");
            UploadData uploadData = new UploadData();
            if (modelType.equals("Excel")) {
//                ExcelUtil excelUtil = new ExcelUtil();
//                excelUtil.setFile(file);
//                JSONObject jsonObj = excelUtil.toJsonObj();
                JSONObject jsonObj = ExcelUtil.toJsonObj(file);
                uploadData.setModel_data(jsonObj);
            }
            for (Object o : projects) {
                JSONObject json = (JSONObject) o;
                if (c.getProjectName().equals(json.getString("project_name"))) {
                    String project_id = json.getString("project_id");
                    uploadData.setProject_id(project_id);
                }
            }
            uploadData.setModel_id(c.getModelId());
            uploadData.setUser_id(userId);
            uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            uploadData.setFile_name(userId + "_" + modelType + "_" + sdf.format(new Date()) + ".json");//包含后缀的文件名
            uploadData.setModel_name(c.getFileName());//不包含后缀
            if (cover.equals("0")) {
                //覆盖原版本模型
                uploadData.setCover(true);
            } else {
                //创建新版本模型
                uploadData.setCover(false);
            }

            uploadData.setModel_name(file.getName().substring(0, file.getName().lastIndexOf('.')));
            uploadData.setDescription(description);
            uploadData.setModel_type(modelType);


            String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
            JSONObject jsonObject = JSONObject.parseObject(jsonString);
            System.out.println(jsonObject);
            //将文件信息及数据发送到rabbitmq中
            messageProducer.sendMessage(jsonObject);

            //等待netty的response,并根据返回的code处理
            NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
            nettyClientHandler.sendMessage();
//            try {
            nettyClientHandler.waitForResponse();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            String response = nettyClientHandler.getResponse();
            System.out.println("[upload response]: " + response);
            JSONObject jsonResponse = JSONObject.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            if (code == 210) {
                ConfigUtil.updateUploadDate(Path.of(filePath));//更新文件上传时间
                ConfigUtil.updateConfig(Path.of(filePath));//更新文件修改时间
                ConfigUtil.updateDescription(Path.of(filePath), description);
                System.out.println("上传成功");
                jsonResponse.put("config", ConfigUtil.getConfigDataByUserId(userId));
                return jsonResponse;
            } else if (code == 211) {
                System.out.println("上传失败");
                return jsonResponse;
            } else if (code == 500401) {
                System.out.println("上传失败：该项目中模型已存在");
                return jsonResponse;
            } else {
                return jsonResponse;
            }
        } else {
            return new JSONObject();
        }
    }

    public JSONObject upload1(String userId, JSONObject loginRes, String filePath, String cover, String description, String modelType, String projectName, String modelId, String modelName) throws IOException, InterruptedException {
        File file = new File(filePath);
        //filePath下模型存在且模型已上传过
        JSONObject data = loginRes.getJSONObject("data");
        JSONArray projects = data.getJSONArray("projects");
        UploadData uploadData = new UploadData();
        if (modelType.equals("Excel")) {
            JSONObject jsonObj = ExcelUtil.toJsonObj(file);
            uploadData.setModel_data(jsonObj);
        }
        for (Object o : projects) {
            JSONObject json = (JSONObject) o;
            if (projectName.equals(json.getString("project_name"))) {
                String project_id = json.getString("project_id");
                uploadData.setProject_id(project_id);
            }
        }
        uploadData.setModel_id(modelId);
        uploadData.setUser_id(userId);
        uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        uploadData.setFile_name(userId + "_" + modelType + "_" + sdf.format(new Date()) + ".json");//包含后缀的文件名
        uploadData.setModel_name(modelName);//不包含后缀
        if (cover.equals("0")) {
            //覆盖原版本模型
            uploadData.setCover(true);
        } else {
            //创建新版本模型
            uploadData.setCover(false);
        }

        uploadData.setModel_name(file.getName().substring(0, file.getName().lastIndexOf('.')));
        uploadData.setDescription(description);
        uploadData.setModel_type(modelType);
        String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        System.out.println(jsonObject);
        //将文件信息及数据发送到rabbitmq中
        messageProducer.sendMessage(jsonObject);

        //等待netty的response,并根据返回的code处理
        NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();

        nettyClientHandler.sendMessage();
        nettyClientHandler.waitForResponse();
        String response = nettyClientHandler.getResponse();

        System.out.println("[upload response]: " + response);
        JSONObject jsonResponse = JSONObject.parseObject(response);
//        Integer code = jsonResponse.getInteger("code");
//        if (code == 210) {
//            ConfigUtil.updateUploadDate(Path.of(filePath));//更新文件上传时间
//            ConfigUtil.updateConfig(Path.of(filePath));//更新文件修改时间
//            ConfigUtil.updateDescription(Path.of(filePath), description);
//            System.out.println("上传成功");
//            jsonResponse.put("config", ConfigUtil.getConfigDataByUserId(userId));
//            return jsonResponse;
//        } else if (code == 211) {
//            System.out.println("上传失败");
//            return jsonResponse;
//        } else if (code == 500401) {
//            System.out.println("上传失败：该项目中模型已存在");
//            return jsonResponse;
//        } else {
//            return jsonResponse;
//        }
        return jsonResponse;
    }

    public JSONObject uploadNew(String userId, JSONObject loginRes, String projectName, String filePath, String description, String modelType) throws IOException, InterruptedException, ParseException {
        //获取文件名(不带后缀),获取文件类型(文件扩展名)
        File file = new File(filePath);
        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
        String fileType = file.getName().substring(file.getName().lastIndexOf(".") + 1);

        //获取登录数据
        JSONObject data = loginRes.getJSONObject("data");
        //获取用户的项目列表
        JSONArray projects = data.getJSONArray("projects");
        UploadData uploadData = new UploadData();
        if (modelType.equals("Excel") && fileType.equals("xlsx")) {
            //将excel转成json数据格式
//            ExcelUtil excelUtil = new ExcelUtil();
//            excelUtil.setFile(file);
//            JSONObject jsonObj = excelUtil.toJsonObj();
            JSONObject jsonObj = ExcelUtil.toJsonObj(file);
            uploadData.setModel_data(jsonObj);
        } else {
            JSONObject jsonObj = new JSONObject();
            uploadData.setModel_data(jsonObj);
        }

        //再projects中找到项目名称为projectName的project_id
        for (Object obj : projects) {
            if (obj instanceof JSONObject) {
                JSONObject json = (JSONObject) obj;
                if (projectName.equals(json.getString("project_name"))) {
                    String project_id = json.getString("project_id");
                    uploadData.setProject_id(project_id);
                }
            }
        }

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        uploadData.setModel_type(modelType);
        uploadData.setFile_name(userId + "_" + modelType + "_" + sdf1.format(new Date()) + ".json");//包含后缀的文件名
        uploadData.setUser_id(userId);
        uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
        uploadData.setModel_name(fileName);//不包含后缀
//        uploadData.setModel_name(modelName);//不包含后缀
        uploadData.setCover(true);//改为true和false,有modelId时,true为覆盖,false创建新版本,没有modelId时,都是添加新模型
        uploadData.setModel_name(file.getName().substring(0, file.getName().lastIndexOf('.')));
        uploadData.setDescription(description);
        String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        //将文件信息及数据发送到rabbitmq中
        messageProducer.sendMessage(jsonObject);

        //等待netty的response,并根据返回的code处理
        NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
        nettyClientHandler.sendMessage();
//        try {
        nettyClientHandler.waitForResponse();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        String response = nettyClientHandler.getResponse();
        System.out.println("[upload response]: " + response);

        JSONObject jsonResponse = JSONObject.parseObject(response);
        Integer code = jsonResponse.getInteger("code");
        if (code == 210) {
            //上传成功后,根据返回response,修改配置文件
            Content content = new Content();
            content.setFilePath(filePath);
            content.setDescription(description);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
            Date modifiedDate = fileChangeService.getModifiedDate(Path.of(filePath));
            content.setModifiedDate(sdf.format(modifiedDate));

            content.setUploadDate(sdf.format(new Date()));
            content.setFileType(fileType);
            content.setFileName(fileName);
//            content.setFileName(modelName);
            content.setUserId(userId);
            content.setProjectName(projectName);

//            fileChangeService.updateUploadDate(Path.of(filePath));
            ConfigUtil.updateUploadDate(Path.of(filePath));
            JSONArray dataArray = jsonResponse.getJSONArray("data");
            for (Object o : dataArray) {
                JSONObject j = (JSONObject) o;
                String modelId = j.getString("modelId");
                content.setModelId(modelId);
                ConfigUtil.addContent(content);
//                fileChangeService.updateModelId(Path.of(filePath), modelId);
            }
            //将修改好的配置文件,传给前端渲染
            jsonResponse.put("config", ConfigUtil.getConfigDataByUserId(userId));
            System.out.println("上传成功");
            return jsonResponse;
        } else if (code == 211) {
            System.out.println("上传失败");
            return jsonResponse;
        } else if (code == 500401) {
            System.out.println("上传失败：该项目中模型已存在");
            return jsonResponse;
        } else {
            return jsonResponse;
        }
    }

    public JSONObject uploadNew1(String modelName, String userId, JSONObject loginRes, String projectName, String filePath, String description, String modelType) throws IOException, InterruptedException, ParseException {
        //获取文件名(不带后缀),获取文件类型(文件扩展名)
        File file = new File(filePath);
//        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
        String fileType = file.getName().substring(file.getName().lastIndexOf(".") + 1);

        //获取登录数据
        JSONObject data = loginRes.getJSONObject("data");
        //获取用户的项目列表
        JSONArray projects = data.getJSONArray("projects");
        UploadData uploadData = new UploadData();
        if (modelType.equals("Excel") && fileType.equals("xlsx")) {
            //将excel转成json数据格式
            JSONObject jsonObj = ExcelUtil.toJsonObj(file);
            uploadData.setModel_data(jsonObj);
        } else {
            JSONObject jsonObj = new JSONObject();
            uploadData.setModel_data(jsonObj);
        }

        //在projects中找到项目名称为projectName的project_id
        for (Object obj : projects) {
            if (obj instanceof JSONObject) {
                JSONObject json = (JSONObject) obj;
                if (projectName.equals(json.getString("project_name"))) {
                    String project_id = json.getString("project_id");
                    uploadData.setProject_id(project_id);
                }
            }
        }

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        uploadData.setModel_type(modelType);
        uploadData.setFile_name(userId + "_" + modelType + "_" + sdf1.format(new Date()) + ".json");//包含后缀的文件名
        uploadData.setUser_id(userId);
//        try {
//            // 使用一个提供公网IP查询服务的网站
//            URL url = new URL("http://checkip.amazonaws.com");
//            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
//
//            // 读取服务返回的公网IP地址
//            String publicIP = br.readLine();
//            System.out.println("Public IP Address: " + publicIP);
//            uploadData.setUser_ip(publicIP);
////            uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        uploadData.setUser_ip(InetAddress.getLocalHost().getHostAddress());

        uploadData.setModel_name(modelName);
        uploadData.setCover(true);//改为true和false,有modelId时,true为覆盖,false创建新版本,没有modelId时,都是添加新模型
        uploadData.setDescription(description);
        String jsonString = JSON.toJSONString(uploadData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        //将文件信息及数据发送到rabbitmq中
        messageProducer.sendMessage(jsonObject);

        //等待netty的response,并根据返回的code处理
        NettyClientHandler nettyClientHandler = nettyClientService.getNettyClientHandler();
        nettyClientHandler.sendMessage();
        nettyClientHandler.waitForResponse();
        String response = nettyClientHandler.getResponse();
        System.out.println("[upload response]: " + response);

        JSONObject jsonResponse = JSONObject.parseObject(response);
        Integer code = jsonResponse.getInteger("code");
        if (code == 210) {
            //上传成功后,根据返回response,修改配置文件
//            Content content = new Content();
//            content.setFilePath(filePath);
//            content.setDescription(description);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
//            Date modifiedDate = fileChangeService.getModifiedDate(Path.of(filePath));
//            content.setModifiedDate(sdf.format(modifiedDate));

//            content.setUploadDate(sdf.format(new Date()));
//            content.setFileType(fileType);
//            content.setFileName(fileName);
//            content.setFileName(modelName);
//            content.setUserId(userId);
//            content.setProjectName(projectName);

//            fileChangeService.updateUploadDate(Path.of(filePath));
//            ConfigUtil.updateUploadDate(Path.of(filePath));
//            JSONArray dataArray = jsonResponse.getJSONArray("data");
//            for (Object o : dataArray) {
//                JSONObject j = (JSONObject) o;
//                String modelId = j.getString("modelId");
//                content.setModelId(modelId);
//                ConfigUtil.addContent(content);
//                fileChangeService.updateModelId(Path.of(filePath), modelId);
//            }
            //将修改好的配置文件,传给前端渲染
//            jsonResponse.put("config", ConfigUtil.getConfigDataByUserId(userId));
            JSONObject uploadRes=new JSONObject();
            uploadRes.put("modelName", uploadData.getModel_name());
            JSONArray dataArray = jsonResponse.getJSONArray("data");
            for (Object o : dataArray) {
                JSONObject j = (JSONObject) o;
                String modelId = j.getString("modelId");
                uploadRes.put("modelId", modelId);
            }
            uploadRes.put("createTime",uploadData.getDate());
            uploadRes.put("updateTime",uploadData.getDate());
            uploadRes.put("projectId",uploadData.getProject_id());
            System.out.println("上传成功");
            jsonResponse.put("uploadRes",uploadRes);
            return jsonResponse;
        } else if (code == 211) {
            System.out.println("上传失败");
            return jsonResponse;
        } else if (code == 500401) {
            System.out.println("上传失败：该项目中模型已存在");
            return jsonResponse;
        } else {
            return jsonResponse;
        }
    }


}
