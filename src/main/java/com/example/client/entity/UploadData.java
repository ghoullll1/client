package com.example.client.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Date;

@Data
public class UploadData {
    private String user_id="";
    private String user_ip="";
    private String model_type="";
    private String model_id="";
    private String model_name="";
    private String project_id="";
    private String file_name="";
    private boolean cover=true;
    private String date=new Date().toString();
    private JSONObject model_data;
    private String description="";
}
