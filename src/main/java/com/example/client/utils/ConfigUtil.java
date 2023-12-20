package com.example.client.utils;

import com.example.client.entity.ConfigData;
import com.example.client.entity.Content;
import com.example.client.entity.TxtData;
import com.example.client.entity.XlsxData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ConfigUtil {
    private static String path="D:\\Desktop\\demo\\client\\src\\main\\resources\\config.json";

    public static void setPath(String path) {
        ConfigUtil.path = path;
    }

    //获取json文件并解析成java实例
    public static ConfigData  getConfigData() throws IOException {
        // 获取JSON文件的路径
        File configFile = new File(path);
        ObjectMapper objectMapper = new ObjectMapper();
        //将json文件转换成java实例
        return objectMapper.readValue(configFile, ConfigData.class);
    }

    //根据filePath获取config中的content
    public static Content getContentByFilePath(String filePath) throws IOException {
        ConfigData configData = getConfigData();
        ArrayList<String> fileType = configData.getFileType();
        if (fileType.contains("excel")) {
            XlsxData xlsx = configData.getExcel();
            for (Content c : xlsx.getContent()) {
                if (c.getFilePath().equals(filePath)) {
                    return c;
                }
            }
        }else if (fileType.contains("txt")) {
            TxtData txt = configData.getTxt();
            for (Content c : txt.getContent()) {
                if (c.getFilePath().equals(filePath)) {
                    return c;
                }
            }
        }
        return null;
    }

    //根据modelId获取config中的content
    public static Content getContentByModelId(String modelId) throws IOException {
        ConfigData configData = getConfigData();
        ArrayList<String> fileType = configData.getFileType();
        if (fileType.contains("excel")) {
            XlsxData xlsx = configData.getExcel();
            for (Content c : xlsx.getContent()) {
                if (c.getModelId().equals(modelId)) {
                    return c;
                }
            }
        }else if (fileType.contains("txt")) {
            TxtData txt = configData.getTxt();
            for (Content c : txt.getContent()) {
                if (c.getModelId().equals(modelId)) {
                    return c;
                }
            }
        }
        return null;
    }

    //添加新的content
    public static void addContent(Content content) throws IOException {
        String fileType = content.getFileType();
        ConfigData configData = getConfigData();
        ObjectMapper objectMapper = new ObjectMapper();
        if(fileType.equals("xlsx")){
            XlsxData xlsx = configData.getExcel();
            ArrayList<Content> xlsxContent = xlsx.getContent();
            xlsxContent.add(content);
            Files.write(Paths.get(path), objectMapper.writeValueAsString(configData).getBytes());
        }
    }
    
    public static ConfigData getConfigDataByUserId(String userId) throws IOException {
        ConfigData configData = getConfigData();

        XlsxData excel = configData.getExcel();
        ArrayList<Content> excelList = excel.getContent();
        excelList.removeIf(c -> !c.getUserId().equals(userId));

        TxtData txt = configData.getTxt();
        ArrayList<Content> txtList = txt.getContent();
        txtList.removeIf(c -> !c.getUserId().equals(userId));
        return configData;
    }

    public static void updateContent(Content content) throws IOException {
        String fileType = content.getFileType();
        ConfigData configData = getConfigData();
        ObjectMapper objectMapper = new ObjectMapper();
        if(fileType.equals("xlsx")){
            XlsxData xlsx = configData.getExcel();
        }
    }
}
