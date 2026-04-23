package com.sidesignal;

import org.springframework.boot.SpringApplication;

public class TestSideSignalApplication {

    public static void main(String[] args) {
        SpringApplication.from(SideSignalApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
