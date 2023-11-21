package com.example.client.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class YourDataEncoder extends MessageToByteEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String data, ByteBuf out) throws Exception {
        // 在这里编码数据并写入输出缓冲区
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}