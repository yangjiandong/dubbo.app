package org.ssh.dubbo.demo.dao.mybatis;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ssh.pm.orm.mybatis.MyBatisRepository;

import com.ek.mobileapp.model.Frequency;
import com.ek.mobileapp.model.OrderData;

@MyBatisRepository
public interface MDataDao {

    @Select("SELECT * From Mob_OrderData "
            + "where patientId = #{patientId} and orderId = #{orderId}")
    public List<OrderData> getOrderDatas(Map<String, Object> map);

    @Select("SELECT * FROM Mob_Frequency WHERE code = #{code}")
    public Frequency getFrequency(Map<String, Object> map);

    @Update("UPDATE Mob_OrderData SET value1=#{value2}, endState = #{endState}, isShow = #{isShow} WHERE id= #{id}")
    public Integer updateOrder(OrderData orderData);
}
