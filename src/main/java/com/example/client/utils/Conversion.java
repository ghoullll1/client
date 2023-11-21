package com.example.client.utils;

import com.alibaba.fastjson.JSONObject;

import java.io.*;

public class Conversion
{
    public byte[] jsonToByte(JSONObject jsonObject) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(jsonObject);
        oos.flush();
        byte[] bytes = bos.toByteArray();

        oos.close();
        bos.close();
        return bytes;
    }

    public JSONObject byteToJson(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        JSONObject jsonObject = (JSONObject)ois.readObject();

        bis.close();
        ois.close();
        return jsonObject;
    }
}
