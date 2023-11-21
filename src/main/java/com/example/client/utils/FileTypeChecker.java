package com.example.client.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileTypeChecker {

    private static Map<String, String> fileTypes = new HashMap<>();

    static {
        // 添加文件类型的映射关系，这里使用文件扩展名作为键
        fileTypes.put("txt", "文本文件");
        fileTypes.put("xlsx", "Excel");
    }

    //根据文件路径获取文件类型
    public static String getFileTypeDescription(String filePath) {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return "文件不存在或不是有效文件";
        }

        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        String fileTypeDescription = fileTypes.get(fileExtension.toLowerCase());

        if (fileTypeDescription != null) {
            return fileTypeDescription;
        } else {
            return "未知文件类型";
        }
    }

}
