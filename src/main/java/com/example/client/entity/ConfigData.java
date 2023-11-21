package com.example.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigData {
    private ArrayList<String> fileType;
    private XlsxData  excel;
    private TxtData  txt;
}
