package org.ssh.dubbo.demo.consumer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ssh.dubbo.demo.DemoService;
import org.ssh.dubbo.demo.NurseMobService;
import org.ssh.dubbo.demo.model.Hz;

import com.ek.mobileapp.model.OrderData;
import com.ek.mobileapp.model.Patient;

//@Component
public class DemoAction {

    //@Reference(version="1.0.0")
    private DemoService<Hz> demoService;

    NurseMobService nurseMobService;

    public void setNurseMobService(NurseMobService nurseMobService) {
        this.nurseMobService = nurseMobService;
    }

    public void setDemoService(DemoService<Hz> demoService) {
        this.demoService = demoService;
    }

    public void start() throws Exception {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                String hello = demoService.sayHello("world" + i);
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + hello);

                System.out.println("current:" + (i + 1));
                List<Hz> alls = demoService.getPageItems(i + 1);
                for (Hz hz : alls) {
                    System.out.println(hz.getHz());
                }

                //get patients
                List<Patient> ps = this.nurseMobService.getPatientAll(4L, "H2028");
                for (Patient patient : ps) {
                    System.out.println(patient.getPatientId() + "," + patient.getPatientName());

                    List<OrderData> ods = this.nurseMobService.getOrderDataFromHisTest(patient.getPatientId());
                    for (OrderData orderData : ods) {
                        System.out.println(">>>>>" + orderData.getOrderNo() + "," + orderData.getOrderText());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
    }

}