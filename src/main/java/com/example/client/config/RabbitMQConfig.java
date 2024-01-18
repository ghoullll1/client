package com.example.client.config;



import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue myQueue() {
        return new Queue("hello", false);
    }
//    @Bean
//    public Queue myQueue() {
//        return new Queue("test", false);
//    }
}

