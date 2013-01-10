package org.ssh.dubbo.demo.consumer;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.ssh.dubbo.demo.DemoService;
import org.ssh.dubbo.demo.model.Hz;


public class DemoAction {

    private DemoService<Hz> demoService;

    public void setDemoService(DemoService<Hz> demoService) {
        this.demoService = demoService;
    }

    public void start() throws Exception {
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            try {
                String hello = demoService.sayHello("world" + i);
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + hello);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
    }

}