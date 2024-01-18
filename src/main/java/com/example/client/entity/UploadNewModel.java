package com.example.client.entity;

import lombok.Data;

@Data
public class UploadNewModel {
    String projectName;
    String filePath;
    String description;
    String modelType;
}
