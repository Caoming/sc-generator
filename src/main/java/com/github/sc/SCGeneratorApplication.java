package com.github.sc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

@SpringBootApplication
@EnableZuulProxy
@EnableWebSocket
public class SCGeneratorApplication {

    public static void main(String[] args) throws IOException {
        //pid
        String name = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        FileWriter writer = new FileWriter(new File("SCGeneratorApplication.pid"));
        writer.write(name);
        writer.close();
        SpringApplication.run(SCGeneratorApplication.class, args);
    }
}
