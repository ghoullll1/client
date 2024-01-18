package com.example.client.utils;

import com.example.client.entity.ConfigData;
import com.example.client.entity.Content;
import com.example.client.entity.TxtData;
import com.example.client.entity.XlsxData;
import com.example.client.service.FileChangeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ConfigUtil {
    private static String path="src/main/resources/config.json";
    public static void setPath(String path) {
        ConfigUtil.path = path;
    }

    //获取json文件并解析成java实例
    public static ConfigData getConfigData() throws IOException {
        System.out.println(path);
        // 获取JSON文件的路径
        File configFile = new File(path);
//        InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream("config.json");
        ObjectMapper objectMapper = new ObjectMapper();
        //将json文件转换成java实例
        return objectMapper.readValue(configFile, ConfigData.class);
    }

    //根据filePath获取config中的content,如果filePath不存在或者filePath中的文件不在configData中则返回null
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
        } else if (fileType.contains("txt")) {
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
        } else if (fileType.contains("txt")) {
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
        if (fileType.equals("xlsx")) {
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
        if (fileType.equals("xlsx")) {
            XlsxData xlsx = configData.getExcel();
        }
    }

    //修改配置文件中的文件修改时间
    public static void updateConfig(Path fullPath) throws IOException {
        ConfigData configData=getConfigData();
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                try {
                    //读取fullPath下的文件参数
                    BasicFileAttributes attributes = java.nio.file.Files.readAttributes(fullPath, BasicFileAttributes.class);
                    //获取文件修改的时间
                    FileTime fileTime = attributes.lastModifiedTime();

                    // 将FileTime转换为Date类型
                    Date modifiedDate = new Date(fileTime.toMillis());

                    // 格式化修改时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
                    String formattedDate = sdf.format(modifiedDate);
                    content.setModifiedDate(formattedDate);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String newJson = objectMapper.writeValueAsString(configData);
                    Files.write(Paths.get(path), newJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //更新文件上传时间
    public static void updateUploadDate(Path fullPath) throws IOException {
        ConfigData configData=getConfigData();
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
                content.setUploadDate(sdf.format(new Date()));
                ObjectMapper objectMapper = new ObjectMapper();
                Files.write(Paths.get(path), objectMapper.writeValueAsString(configData).getBytes());
            }
        }
    }

    //更新模型描述
    public static void updateDescription(Path fullPath, String description) throws IOException {
        ConfigData configData=getConfigData();
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                try {
                    content.setDescription(description);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String newJson = objectMapper.writeValueAsString(configData);
                    Files.write(Paths.get(path), newJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
