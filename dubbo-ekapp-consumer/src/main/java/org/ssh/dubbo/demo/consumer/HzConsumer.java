package org.ssh.dubbo.demo.consumer;

import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.ssh.dubbo.demo.DemoService;
import org.ssh.dubbo.demo.model.Hz;

public class HzConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "META-INF/spring/dubbo-demo-consumer.xml" });
        context.start();

        DemoService<Hz> demo = (DemoService<Hz>) context.getBean("demoService");
        for (int i = 0; i < 100; i++) {
            System.out.println("current:" + (i + 1));
            List<Hz> alls = demo.getPageItems(i + 1);
            for (Hz hz : alls) {
                System.out.println(hz.getHz());
            }
        }

    }
}
