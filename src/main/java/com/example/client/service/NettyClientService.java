package com.example.client.service;

import com.example.client.utils.NettyClientHandler;
import com.example.client.utils.YourDataEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class NettyClientService {
    @Value("${netty.client.host}")
    private String host;

    @Value("${netty.client.port}")
    private int port;
    private CountDownLatch initLatch = new CountDownLatch(1);
    private NettyClientHandler nettyClientHandler;

//    @PostConstruct
    public void  init() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        nettyClientHandler = new NettyClientHandler();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加处理器和处理逻辑
                        pipeline.addLast(new IdleStateHandler(0, 1, 0, TimeUnit.SECONDS));
                        pipeline.addLast("handler", nettyClientHandler);
                    }
                });
        Runnable task=()->{
            try {
                ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
                initLatch.countDown(); // 减少 latch 计数
                channelFuture.channel().closeFuture().sync();
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }finally {
                group.shutdownGracefully();
            }
        };
        Thread thread=new Thread(task);
        thread.start();
    }
    //向服务端发起请求
    public void sendRequest(String requestData) throws InterruptedException {
        initLatch.await(); // 等待 init 方法中的线程完成
        nettyClientHandler.sendMessage(requestData);
        nettyClientHandler.getResponse();
    }

    //发送Data到Electron中
    public void sendDataToElectron(String host, int port, String requestData) {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port)) // 应用程序的主机和端口
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加处理器和处理逻辑，用于编码和发送数据
//                            pipeline.addLast(new YourDataEncoder()); // 自定义数据编码器
                            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                        }
                    });

            // 连接到Electron应用程序
            Channel channel = bootstrap.connect().sync().channel();

            // 向Electron发送数据
            channel.writeAndFlush(requestData);

            // 关闭连接
            channel.close().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public NettyClientHandler getNettyClientHandler() {
        return nettyClientHandler;
    }

}
