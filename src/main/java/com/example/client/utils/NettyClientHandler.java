package com.example.client.utils;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext ctx; // 保存通道上下文，以便后续使用

    // 控制等待回应的操作
    private CountDownLatch latch;
    // 定义心跳事件的间隔时间（单位：秒）
    private static final int HEARTBEAT_INTERVAL = 30;
    //服务端的响应结果
    private String response;
    //响应队列
    private BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(10); // 队列用于存储响应
    // 客户端请求的心跳命令
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("hb_request", CharsetUtil.UTF_8));
    private int fcount = 1;  // 心跳循环次数
    private String requestData;
    private String userID;

    private int messageCount = 0;

    private CountDownLatch ctxLatch=new CountDownLatch(1);

    public NettyClientHandler() {
    }

    public NettyClientHandler(String requestData) {
        this.requestData = requestData;
    }

    // 通道激活时运行
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctxLatch.countDown();
        this.ctx = ctx; // 保存通道上下文
    }

    //用于实现心跳连接
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("[INFO] count: " + fcount);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                // 如果客户端在指定时间内没有写入数据，则发送心跳消息
                this.ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(future -> {
                    if (!future.isSuccess()) {
                        // 心跳发送失败，可以处理重连逻辑等
                        System.err.println("Heartbeat failed: " + future.cause());
                        this.ctx.close(); // 关闭连接
                    }
                });
                fcount++;
            }
        }
    }

    //通道响应内容读取
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException, JsonProcessingException {
        // 处理从服务器接收到的响应数据
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            this.response = byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
        }
        JSONObject response = JSONObject.parseObject(this.response);
        Integer code = response.getInteger("code");
        //处理文件修改
        if (code == 4100) {
            String modelType = response.getJSONObject("data").getString("modelType");
            if (modelType.equals("Excel")) {
                String modelId = response.getJSONObject("data").getString("modelId");
                JSONArray modefiedList = response.getJSONObject("data").getJSONArray("modefiedList");
                for (int i = 0; i < modefiedList.size(); i++) {
                    JSONObject modefied = modefiedList.getJSONObject(i);
                    String sheetName = modefied.getString("sheetName");
                    String value = modefied.getString("value");
                    String col = modefied.getString("col");
                    int row = Integer.parseInt(modefied.getString("row"));
                    int isSuccess = ExcelUtil.updateExcel(modelId, sheetName, row, col, value);
                    System.out.println(isSuccess);
                    if (isSuccess==1) {
                        System.out.println("修改成功");
                        Map<String,String> map=new HashMap<>();
                        map.put("code", "200");
                        map.put("method", "responseModifyModeData");
                        map.put("modelId", modelId);
                        ObjectMapper objectMapper = new ObjectMapper();
                        String s = objectMapper.writeValueAsString(map);
                        System.out.println(s);
                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
                    }else if(isSuccess==-1){
                        System.out.println("文件被打开");
                        Map<String,String> map=new HashMap<>();
                        map.put("code", "210");
                        map.put("method", "responseModifyModeData");
                        map.put("modelId", modelId);
                        ObjectMapper objectMapper = new ObjectMapper();
                        String s = objectMapper.writeValueAsString(map);
                        System.out.println(s);
                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
                    }else{
                        System.out.println("修改失败");;
                        Map<String,String> map=new HashMap<>();
                        map.put("code", "211");
                        map.put("method", "responseModifyModeData");
                        map.put("modelId", modelId);
                        ObjectMapper objectMapper = new ObjectMapper();
                        String s = objectMapper.writeValueAsString(map);
                        System.out.println(s);
                        ctx.writeAndFlush(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
                    }
                }
            }
        }
        if (latch.getCount() > 0) {
            latch.countDown();  // 收到服务器的消息的时候，将计数器减1
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 连接断开时执行清理操作，例如用户退出登录
        // 这里可以实现用户退出登录的逻辑
        this.ctx.close();
    }

    //异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 处理异常
        cause.printStackTrace();
        this.ctx.close();
    }

    public void sendMessage() {
        latch = new CountDownLatch(1);  // 发送消息的时候，将计数器的设置为1
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String requestData) {
        latch = new CountDownLatch(1);  // 发送消息的时候，将计数器的设置为1
        try {
            ctxLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ctx.writeAndFlush(Unpooled.copiedBuffer(requestData, CharsetUtil.UTF_8));
    }

    public void waitForResponse() throws InterruptedException {
        // 在等待回应的操作中，调用await方法进行等待，直到计数器为0，即可收到服务端的回应
        boolean success = latch.await(10, TimeUnit.SECONDS);
        if (!success) {
            System.out.println("丢失了服务器的返回的一条消息！！！ \n");
            response = "";
        }
    }

    public void close() {
        ctx.close();
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public String getResponse() {
        return response;
    }
}