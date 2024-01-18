package com.example.client.utils;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.client.entity.ConfigData;
import com.example.client.entity.Content;
import com.example.client.entity.XlsxData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;


public class ExcelUtil {
    //配置文件路径
    private static String path="D:/Desktop/workspace/json/config.json";
    private static File file;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    //将file文件转换为json对象
    public JSONObject toJsonObj1() throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(inputStream);
        JSONObject result = new JSONObject();
        //遍历每个表
        for (Sheet sheet : workbook) {
            int rowspan = sheet.getLastRowNum();
            String sheetName = sheet.getSheetName();
            JSONObject classJson = new JSONObject();
            JSONArray stu = new JSONArray();
            JSONArray title = new JSONArray();
            int colspan = 0;
            //遍历每一行
            for (Row row : sheet) {
                colspan = row.getPhysicalNumberOfCells();
                //第一行为表头
                if (row.getRowNum() == 0) {
                    for (Cell cell : row) {
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("name", cell.toString());
                        jsonObj.put("key", getExcelColumnLabel(cell.getColumnIndex()));
                        title.add(jsonObj);
                    }
                } else {
                    JSONObject jsonObj = new JSONObject();
                    //遍历每一列
                    for (Cell cell : row) {
                        jsonObj.put(getExcelColumnLabel(cell.getColumnIndex()), cell.toString());
                    }
                    stu.add(jsonObj);
                }
            }
            classJson.put("colspan", colspan);
            classJson.put("colsName", title);
            classJson.put("rowspan", rowspan);
            classJson.put("content", stu);
            result.put(sheetName, classJson);
        }
        inputStream.close();
        return result;
    }

    //将file文件转换为json对象
    public static JSONObject toJsonObj(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(inputStream);
        JSONObject result = new JSONObject();
        JSONObject sheetname = new JSONObject();
        //遍历每个表
        for (Sheet sheet : workbook) {
            String sheetName = sheet.getSheetName();
            JSONObject newsheet=new JSONObject();
            JSONArray content = new JSONArray();
            sheetname.put("Sheet"+(workbook.getSheetIndex(sheet)+1), sheetName);
//            sheetname.put(sheetName, newsheet);
            //遍历每一行
            for (Row row : sheet) {
                if(row!=null){
                    for (Cell cell : row) {
                        if(cell!=null){
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("value", cell.toString());
                            jsonObj.put("row", cell.getRowIndex()+1+"");
                            jsonObj.put("col", getExcelColumnLabel(cell.getColumnIndex()));
                            content.add(jsonObj);
                        }else{
                            System.out.println("null");
                        }
                    }
                }
            }
            result.put(sheetName, content);
        }
        result.put("sheetname", sheetname);
        inputStream.close();
        return result;
    }

    //获取指定表格指定行列的值
    public String getExcelData(String sheetName, int row, int column) throws IOException {
        InputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet(sheetName);
        String data = sheet.getRow(row - 1).getCell(column).toString();
        fis.close();
        return data;
    }

    //修改指定表格中指定行列的值
    public static void setExcelData(String filePath,String sheetName, int row, int column, String data) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet(sheetName);
        Cell cell = sheet.getRow(row - 1).getCell(column);
        if(cell==null){
            cell = sheet.getRow(row - 1).createCell(column);
        }
        cell.setCellValue(data);
        fis.close();
        FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        fos.close();
    }

    //  获取列索引转数字
    public static int getExcelColumnNumber(String column) {
        int num = 0;
        for (int i = 0; i < column.length(); i++) {
            char c = column.charAt(i);
            num = num * 26 + (c - 'A' + 1);
        }
        return num - 1;
    }

    //获取列索引数字转字母
    public static String getExcelColumnLabel(int num) {
        String temp = "";
        double i = Math.floor(Math.log(25.0 * (num) / 26.0 + 1) / Math.log(26)) + 1;
        if (i > 1) {
            double sub = num - 26 * (Math.pow(26, i - 1) - 1) / 25;
            for (double j = i; j > 0; j--) {
                temp = temp + (char) (sub / Math.pow(26, j - 1) + 65);
                sub = sub % Math.pow(26, j - 1);
            }
        } else {
            temp = temp + (char) (num + 65);
        }
        return temp;
    }

    //下行修改Excel文件指定内容
    public static  int updateExcel(String modelId,String sheetName,int row,String column,String data)  {
        int isSuccess = 0;//0是失败 1是成功 -1是文件被打开导致失败
        ConfigData configData = null;
        try {
            configData = ConfigUtil.getConfigData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        XlsxData excel = configData.getExcel();
        ArrayList<Content> content = excel.getContent();
        for (Content c : content) {
            if (c.getModelId().equals(modelId)) {
                String filePath = c.getFilePath();
                try {
                    setExcelData(filePath,sheetName,row,getExcelColumnNumber(column),data);
                    isSuccess=1;
                } catch (IOException e) {
                    String message = e.getMessage();
                    System.out.println(message);
                    if(message!=null&&message.contains("另一个程序正在使用此文件，进程无法访问。")){
                        isSuccess=-1;
                    }else{
                        isSuccess=0;
                    }

                }
            }
        }
        return isSuccess;
    }

}
