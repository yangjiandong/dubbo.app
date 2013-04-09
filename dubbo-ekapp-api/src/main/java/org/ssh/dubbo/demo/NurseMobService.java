package org.ssh.dubbo.demo;

import java.util.List;

import com.ek.mobileapp.model.OrderData;
import com.ek.mobileapp.model.Patient;

public interface NurseMobService {
    List<Patient> getPatientAll(Long userId, String departNo);

    List<OrderData> getOrderDataFromHisTest(String patientId);
}
