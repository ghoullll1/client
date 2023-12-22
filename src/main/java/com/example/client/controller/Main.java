package com.example.client.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws JsonProcessingException {
        String jsonData = "YOUR_JSON_DATA_HERE1"; // Replace this with your actual JSON data
        JSONArray result = convertToExcelFormat("D:\\Desktop\\demo\\client\\src\\main\\resources\\excelData.json");
        System.out.println(result);
    }

    //转换excel格式
    public static JSONArray convertToExcelFormat(String path) {
        List<Map<String, Object>> excelDataList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(path);

        try {

            JsonNode rootNode = objectMapper.readTree(file);//获取根节点
            JsonNode modelData = rootNode.get("model_data");
            JsonNode sheetname = modelData.get("sheetname");
            //遍历表名列表
            for (JsonNode jsonNode : sheetname) {
                Map<String, Object> data = new HashMap<>();
                String name=jsonNode.asText();
                data.put("showName", name);
                data.put("sheetName", name);
                ArrayList<Object> childProjects = new ArrayList<>();//存储表名为name的数据
                JsonNode sheet = modelData.get(name);//获取表名为name的表

                String currentRow=null;
                //表非空,就遍历表
                if(sheet!=null) {
                    for (JsonNode dataNode : sheet) {
                        String row = dataNode.get("row").asText();

                        //当前行号和上一行号不相等时，说明是新的行，需要创建一个新的对象
                        if (!row.equals(currentRow)) {
                            currentRow = row;//将行号更新到新的行
                            Map<String, Object> rowMap = new HashMap<>();
                            rowMap.put("showName", "row" + row);
                            rowMap.put("row", row);

                            ArrayList<Object> rowList = new ArrayList<>();//存储第row行元素

                            //遍历第row行元素,并加入到rowList中
                            for (JsonNode node : sheet) {
                                if (node.get("row").asText().equals(row)) {
                                    HashMap<String, Object> rowData = new HashMap<>();
                                    rowData.put("showName", "<" + row + "," + node.get("col").asText() + "> " + node.get("value").asText());
                                    rowData.put("col", node.get("col").asText());
                                    rowData.put("value", node.get("value").asText());
                                    rowList.add(rowData);
                                }
                            }

                            rowMap.put("childProjects", rowList);
                            childProjects.add(rowMap);
                        }

                    }
                    data.put("childProjects", childProjects);

                }

                excelDataList.add(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.parseArray(JSONArray.toJSONString(excelDataList));
        return jsonArray;
    }

    public static List<Map<String, Object>> convertToDesiredFormat1(String jsonData) {
        List<Map<String, Object>> excelDataList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode sheetData = rootNode.get("model_data");

            for (JsonNode sheet : sheetData) {
                String sheetName = sheet.get("Sheet1").asText(); // Assuming processing "Sheet1" data

                List<Map<String, Object>> childProjectsList = new ArrayList<>();
                Map<String, Object> sheetMap = new HashMap<>();
                sheetMap.put("showName", sheetName);
                sheetMap.put("sheetName", sheetName);
                sheetMap.put("childProjects", childProjectsList);

                for (JsonNode item : sheet.get(sheetName)) {
                    String row = item.get("row").asText();
                    String col = item.get("col").asText();
                    String value = item.get("value").asText();

                    Map<String, Object> rowMap = new HashMap<>();
                    rowMap.put("showName", "<" + row + "," + col + "> " + value);
                    rowMap.put("row", row);
                    rowMap.put("col", col);
                    rowMap.put("value", value);

                    Map<String, Object> childProjectMap = new HashMap<>();
                    childProjectMap.put("showName", "row" + row);
                    childProjectMap.put("row", row);
                    List<Map<String, Object>> childProjectChildren = new ArrayList<>();
                    childProjectChildren.add(rowMap);
                    childProjectMap.put("childProjects", childProjectChildren);

                    childProjectsList.add(childProjectMap);
                }

                excelDataList.add(sheetMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return excelDataList;
    }
}
