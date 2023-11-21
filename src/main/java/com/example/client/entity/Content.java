package com.example.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private String projectName;
    private String fileName;
    private String fileType;
    private String filePath;
    private String modifiedDate;
    private String uploadDate;
    private String userId;
    private String description;
    private String modelId;
}
