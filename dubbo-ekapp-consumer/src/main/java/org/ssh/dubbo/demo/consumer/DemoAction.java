package org.ssh.dubbo.demo.consumer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;
import org.ssh.dubbo.demo.DemoService;
import org.ssh.dubbo.demo.model.Hz;

import com.alibaba.dubbo.config.annotation.Reference;

//@Component
public class DemoAction {

    //@Reference(version="1.0.0")
    private DemoService<Hz> demoService;

    public void setDemoService(DemoService<Hz> demoService) {
        this.demoService = demoService;
    }

    public void start() throws Exception {
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            try {
                String hello = demoService.sayHello("world" + i);
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + hello);

                System.out.println("current:" + (i + 1));
                List<Hz> alls = demoService.getPageItems(i + 1);
                for (Hz hz : alls) {
                    System.out.println(hz.getHz());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
    }

}