package com.example.client.service;

import com.alibaba.fastjson.JSONObject;
import com.example.client.utils.Conversion;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;


@Service
public class MessageProducer {
    private final RabbitTemplate rabbitTemplate;
    private final Queue queue;

    @Autowired
    public MessageProducer(RabbitTemplate rabbitTemplate, Queue queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
    }

    //向rabbitmq发送消息
    public void sendMessage(JSONObject json) {//JSONObject json
        Conversion conversion = new Conversion();
        try {
            rabbitTemplate.convertAndSend(queue.getName(), conversion.jsonToByte(json));//conversion.jsonToByte(json)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
