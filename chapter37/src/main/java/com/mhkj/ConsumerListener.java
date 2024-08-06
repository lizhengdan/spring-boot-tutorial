package com.mhkj;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerListener {

    @KafkaListener(topics = "testLocal")
    public void consumeLocal(String message){
        System.out.println("testLocal:" + message);
    }

    @KafkaListener(topics = "testRemote")
    public void consumeRemote(String message){
        System.out.println("testRemote:" + message);
    }

}
