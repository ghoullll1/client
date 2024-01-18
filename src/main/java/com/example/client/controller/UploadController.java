package com.example.client.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.client.entity.Content;
import com.example.client.entity.LoginData;
import com.example.client.entity.UploadData;
import com.example.client.entity.UploadNewModel;
import com.example.client.service.FileUploadService;
import com.example.client.utils.ConfigUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private final LoginData loginData;

    @Autowired
    FileUploadService fileUploadService;

    @Autowired
    public UploadController(LoginData loginData) {
        this.loginData = loginData;
    }

//    String projectName, String filePath,String description,String modelType
    @PostMapping("/newModel")
    public JSONObject uploadNewModel(String modelName,String projectName, String filePath,String description,String modelType){
        try {
            JSONObject uploadNewRes = fileUploadService.uploadNew1(modelName,loginData.getUserID(), loginData.getLoginRes(), projectName, filePath, description, modelType) ;
            return uploadNewRes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/oldModel")
    public JSONObject uploadOldModel(String filePath,String cover,String description,String modelType){
        try {
            Content content = ConfigUtil.getContentByFilePath(filePath);
            if(content != null){
                JSONObject uploadOldRes=fileUploadService.upload(loginData.getUserID(), loginData.getLoginRes(), filePath, cover, description, modelType);
                return uploadOldRes;
            }else{
                System.out.println("文件不存在");
                JSONObject uploadOldRes=new JSONObject();
                return uploadOldRes;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
