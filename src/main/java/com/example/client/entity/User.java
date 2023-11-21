package com.example.client.entity;

import lombok.Data;

@Data
public class User {
    private String account;
    private String password;
    // 判断 User 对象中的属性是否有任何一个为空或为 null
    public boolean isUserEmpty() {
        return account == null || account.trim().isEmpty() ||
                password == null || password.trim().isEmpty();
    }
}
