package org.ssh.dubbo.demo.provider;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.ssh.dubbo.demo.NurseMobService;
import org.ssh.dubbo.demo.dao.JdbcDao;
import org.ssh.dubbo.demo.dao.mybatis.MDataDao;
import org.ssh.dubbo.demo.utils.AppUtil;
import org.ssh.dubbo.demo.utils.JDBCUtil;

import com.ek.mobileapp.dubbo.DbResource;
import com.ek.mobileapp.dubbo.ItemSource;
import com.ek.mobileapp.model.Frequency;
import com.ek.mobileapp.model.MobConstants;
import com.ek.mobileapp.model.OrderData;
import com.ek.mobileapp.model.Patient;
import com.google.common.collect.Maps;

public class NurseMobServiceImpl implements NurseMobService {
    static com.alibaba.dubbo.common.logger.Logger logger = com.alibaba.dubbo.common.logger.LoggerFactory
            .getLogger(NurseMobServiceImpl.class);
    JdbcTemplate jdbcTemplate;
    JdbcDao jdbcDao;
    MDataDao mDataDao;

    @SuppressWarnings("rawtypes")
    public List<Patient> getPatientAll(Long userId, String departNo) {
        logger.debug("user:" + userId + ",departNo:" + departNo);
        System.out.println("user:" + userId + ",d:" + departNo);

        List<Patient> list = new ArrayList<Patient>();
        try {
            Map<Integer, Object> map = new HashMap<Integer, Object>();
            map.put(1, departNo);

            List<Map> resultList = execSp(3L, map);
            for (Map<String, Object> a : resultList) {
                Patient entity = new Patient();
                entity.setPatientId((String) a.get("PATIENTID"));
                entity.setPatientName((String) a.get("PATIENTNAME"));
                entity.setAge((String) a.get("AGE"));
                entity.setBedNo((String) a.get("BEDNO"));
                entity.setSex((String) a.get("SEX"));
                entity.setWardCode((String) a.get("DEPTCODE"));
                entity.setDeptName((String) a.get("DEPTNAME"));
                entity.setDoctorName((String) a.get("DOCTORNAME"));
                entity.setCondition((String) a.get("CONDITION"));
                entity.setNurseClass((String) a.get("NURSECLASS"));
                entity.setIsNewOrder((String) a.get("ISNEWORDER"));
                entity.setAdmissDate((String) a.get("ADMISSDATE"));
                entity.setDiagIncome((String) a.get("DIAGINCOME"));
                entity.setDiseaseCodeIncome((String) a.get("DISEASECODEINCOME"));
                entity.setUserId(userId);
                list.add(entity);
            }

        } catch (Exception e) {
            logger.error("getPatientAll:", e);
            //throw new Exception(e);
        }
        return list;
    }

    public List<OrderData> getOrderDataFromHisTest(String patientId) {

        String msg = "开始提取医嘱,住院号:<" + String.valueOf(patientId) + "> ";
        logger.debug(msg);
        System.out.println(msg);

        List<OrderData> list = new ArrayList<OrderData>();
        String busDate = jdbcDao.getNowString("yyyy-MM-dd");
        try {
            Map<Integer, Object> map = new HashMap<Integer, Object>();
            map.put(1, patientId);
            map.put(2, busDate);

            List<Map> resultList = execSp(16L, map);
            msg = "提取到医嘱，住院号:<" + String.valueOf(patientId) + "> ";
            logger.debug(msg);
            System.out.println(msg);

            list = processHisOrders(busDate, resultList, "");

            boolean isOutPatient = false;
            for (OrderData b : list) {
                if (b.getOrderType() == MobConstants.ORDER_TYPE_LONG) {
                    Map<String, Object> parameters = Maps.newHashMap();
                    parameters.put("code", b.getFrequency());
                    Frequency f = mDataDao.getFrequency(parameters);
                    if (f != null) {
                        b.setValue1(f.getValue());
                    }
                }

                //长期医嘱依据频次来计算一天执行多次
                //processTimes(b);
                if (b.getEndState().equals(MobConstants.MOB_INACTIVE) && b.getValue1() == b.getValue2()) {
                    b.setEndState(MobConstants.MOB_ACTIVE);
                }

                if (b.getOrderText().equals("出院"))
                    isOutPatient = true;
                else
                    isOutPatient = false;

                boolean flag = false;
                if (b.getOrderType() == MobConstants.ORDER_TYPE_LONG) {
                    for (Map<String, Object> a : resultList) {
                        if (b.getOrderNo().equals(String.valueOf(a.get("ORDERNO")))) {
                            //当天停止,如果当前时间小于停止时间,则显示
                            //如果大于停止时间且这个医嘱未执行过,则不显示
                            if (StringUtils.isBlank(b.getEndDateTime())) {
                                flag = true;
                            } else {
                                if (jdbcDao.getNowString("yyyy-MM-dd HH:mm:ss").compareTo(b.getEndDateTime()) > 0
                                        && b.getValue2() == 0 && isOutPatient == false)
                                    flag = false;
                                else
                                    flag = true;
                            }
                            break;
                        }
                    }
                } else {
                    flag = true;
                }

                if (flag) {
                    b.setIsShow(MobConstants.MOB_ACTIVE);
                } else {
                    b.setIsShow(MobConstants.MOB_INACTIVE);
                }
                mDataDao.updateOrder(b);
            }

        } catch (Exception e) {
            logger.error("getOrderData:", e);
            //throw new Exception(e);
        }

        return list;

    }

    boolean checkOrderNo(List<String> orderNums, String orderNo) {
        for (String o : orderNums) {
            if (o.equals(orderNo))
                return true;
        }
        return false;
    }

    private List<OrderData> processHisOrders(String busDate, List<Map> resultList, String from) throws Exception {
        List<OrderData> list = new ArrayList<OrderData>();

        OrderData entity = null;
        List<String> orderNums = new ArrayList<String>();
        for (Map<String, Object> a : resultList) {

            if (checkOrderNo(orderNums, (String) a.get("ORDERNO"))) {
                continue;
            }
            Map<String, Object> parameters = Maps.newHashMap();
            parameters.put("patientId", (String) a.get("PATIENTID"));
            parameters.put("orderId", (String) a.get("ORDERID"));
            List<OrderData> olist = mDataDao.getOrderDatas(parameters);

            if (olist.size() > 0) {
                //同步已有his医嘱
                entity = olist.get(0);
                entity.setOrderId((String) a.get("ORDERID"));
                entity.setOrderNo((String) a.get("ORDERNO"));
                entity.setOrderSubNo((String) a.get("ORDERSUBNO"));
                entity.setOrderClass((String) a.get("ORDERCLASS"));
                if (a.get("FREQDETAIL") != null && StringUtils.isNotBlank(String.valueOf(a.get("FREQDETAIL")))) {
                    entity.setOrderText((String) a.get("ORDERTEXT") + "(" + String.valueOf(a.get("FREQDETAIL")) + ")");
                    entity.setFreqDetail((String) a.get("FREQDETAIL"));
                } else {
                    entity.setOrderText((String) a.get("ORDERTEXT"));
                    entity.setFreqDetail("");
                }
                if (a.get("DOSAGE") != null) {
                    if (String.valueOf(a.get("DOSAGE")).startsWith(".")) {
                        entity.setDosage("0" + ((String) a.get("DOSAGE")));
                    } else {
                        entity.setDosage((String) a.get("DOSAGE"));
                    }
                }
                entity.setPatientId((String) a.get("PATIENTID"));
                entity.setVisitId((String) a.get("VISITID"));
                entity.setFrequency((String) a.get("FREQUENCY"));
                entity.setAdministration((String) a.get("ADMINISTRATION"));
                entity.setOrderType(Long.valueOf(a.get("ORDERTYPE").toString()));
                entity.setPerformSchedule((String) a.get("PERFORMSCHEDULE"));
                entity.setStartDateTime((String) a.get("STARTDATETIME"));
                if (from.equals("cron")) {
                    entity.setEndDateTime((String) a.get("ENDDATETIME"));
                } else {

                    //保证到本日为止的结束日期,明天的结束日期作为还没结束
                    if (a.get("ENDDATETIME") != null
                            && StringUtils.isNotBlank(String.valueOf(a.get("ENDDATETIME")))
                            && String.valueOf(a.get("ENDDATETIME")).substring(0, 10)
                                    .equals(jdbcDao.getNowString("yyyy-MM-dd")))
                        entity.setEndDateTime((String) a.get("ENDDATETIME"));
                }
            } else {
                entity = new OrderData();
                entity.setOrderId((String) a.get("ORDERID"));
                entity.setOrderNo((String) a.get("ORDERNO"));
                entity.setOrderSubNo((String) a.get("ORDERSUBNO"));
                entity.setOrderClass((String) a.get("ORDERCLASS"));
                if (a.get("FREQDETAIL") != null && StringUtils.isNotBlank(String.valueOf(a.get("FREQDETAIL")))) {
                    entity.setOrderText((String) a.get("ORDERTEXT") + "(" + String.valueOf(a.get("FREQDETAIL")) + ")");
                    entity.setFreqDetail((String) a.get("FREQDETAIL"));
                } else {
                    entity.setOrderText((String) a.get("ORDERTEXT"));
                    entity.setFreqDetail("");
                }

                if (a.get("DOSAGE") != null) {
                    if (String.valueOf(a.get("DOSAGE")).startsWith(".")) {
                        entity.setDosage("0" + ((String) a.get("DOSAGE")));
                    } else {
                        entity.setDosage((String) a.get("DOSAGE"));
                    }
                }
                entity.setPatientId((String) a.get("PATIENTID"));
                entity.setVisitId((String) a.get("VISITID"));
                entity.setFrequency((String) a.get("FREQUENCY"));
                entity.setAdministration((String) a.get("ADMINISTRATION"));
                entity.setOrderType(Long.valueOf(a.get("ORDERTYPE").toString()));
                entity.setPerformSchedule((String) a.get("PERFORMSCHEDULE"));
                entity.setStartDateTime((String) a.get("STARTDATETIME"));
                if (from.equals("cron")) {
                    entity.setEndDateTime((String) a.get("ENDDATETIME"));
                } else {
                    if (a.get("ENDDATETIME") != null
                            && StringUtils.isNotBlank(String.valueOf(a.get("ENDDATETIME")))
                            && String.valueOf(a.get("ENDDATETIME")).substring(0, 10)
                                    .equals(jdbcDao.getNowString("yyyy-MM-dd")))
                        entity.setEndDateTime((String) a.get("ENDDATETIME"));
                }

                entity.setValue1(1);
                entity.setValue2(0);
                entity.setState(MobConstants.MOB_INACTIVE);
                if (entity.getOrderType() == MobConstants.ORDER_TYPE_SHORT
                        && !StringUtils.isBlank(entity.getPerformSchedule())) {
                    //System.out.println("提取完成的临时医嘱:" + entity.getPatientId());

                    //临时医嘱已执行
                    entity.setValue1(1);
                    entity.setValue2(1);
                    entity.setEndState(MobConstants.MOB_ACTIVE);

                }
                entity.setAddDate(jdbcDao.getNowString("yyyy-MM-dd"));
            }

            //重新处理医嘱
            if (entity.getOrderType() == MobConstants.ORDER_TYPE_LONG && entity.getPerformSchedule().contains("周")) {
                String performSchedule = entity.getPerformSchedule();
                int m = 0, n = 0;
                for (int i = 0; i < performSchedule.length(); i++) {
                    String per = performSchedule.substring(i, i + 1);
                    if (per.equals("(")) {
                        m = i;
                    }
                    if (per.equals(")")) {
                        n = i;
                    }
                }
                if (m != 0 && n != 0 && m < n) {
                    String perf = performSchedule.substring(m + 1, n);
                    boolean flag = false;
                    for (int i = 0; i < perf.length(); i++) {
                        String s = String.valueOf(perf.substring(i, i + 1));
                        int dday = AppUtil.dayForWeek(busDate);
                        if (s.equals(String.valueOf(dday))) {
                            flag = true;
                            if (!busDate.equals(entity.getAddDate())) {
                                entity.setValue2(0);
                                entity.setState(MobConstants.MOB_INACTIVE);
                                entity.setAddDate(jdbcDao.getNowString("yyyy-MM-dd"));
                            }
                            entity.setEndState(MobConstants.MOB_INACTIVE);
                            list.add(entity);
                            break;
                        }
                    }

                    if (!flag) {
                        orderNums.add(entity.getOrderNo());
                    }
                }

            } else if (entity.getOrderType() == MobConstants.ORDER_TYPE_LONG
                    && entity.getFrequency().equals(MobConstants.FREQ_Q3D)) {

                Long n = AppUtil.getDayTotal(spellDate(entity.getStartDateTime()), spellDate(busDate));
                Integer m = n.intValue();

                if (m % 3 == 0) {
                    if (!busDate.equals(entity.getAddDate())) {
                        entity.setValue2(0);
                        entity.setState(MobConstants.MOB_INACTIVE);
                        entity.setAddDate(jdbcDao.getNowString("yyyy-MM-dd"));
                    }
                    entity.setEndState(MobConstants.MOB_INACTIVE);
                    list.add(entity);
                    continue;
                } else {
                    orderNums.add(entity.getOrderNo());
                }

            } else if (entity.getOrderType() == MobConstants.ORDER_TYPE_LONG
                    && entity.getFrequency().equals(MobConstants.FREQ_QOD)) {

                Long n = AppUtil.getDayTotal(spellDate(entity.getStartDateTime()), spellDate(busDate));
                Integer m = n.intValue();

                if (m % 2 == 0) {
                    if (!busDate.equals(entity.getAddDate())) {
                        entity.setValue2(0);
                        entity.setState(MobConstants.MOB_INACTIVE);
                        entity.setAddDate(jdbcDao.getNowString("yyyy-MM-dd"));
                    }
                    entity.setEndState(MobConstants.MOB_INACTIVE);
                    list.add(entity);
                    continue;
                } else {
                    orderNums.add(entity.getOrderNo());
                }

            } else {

                if (entity.getOrderType() == MobConstants.ORDER_TYPE_SHORT
                        && !StringUtils.isBlank(entity.getPerformSchedule())) {
                    //System.out.println("提取完成的临时医嘱2:" + entity.getPatientId());
                    //临时医嘱已执行
                    entity.setValue1(1);
                    entity.setValue2(1);
                    entity.setEndState(MobConstants.MOB_ACTIVE);
                } else {
                    if (!busDate.equals(entity.getAddDate())) {
                        entity.setValue2(0);
                        entity.setState(MobConstants.MOB_INACTIVE);
                        entity.setAddDate(jdbcDao.getNowString("yyyy-MM-dd"));
                    }
                    entity.setEndState(MobConstants.MOB_INACTIVE);
                }
                list.add(entity);
            }
        }

        return list;
    }

    @SuppressWarnings("rawtypes")
    protected List<Map> execSp(Long itemId, Map<Integer, Object> map) {
        List<Map> resultList = new ArrayList<Map>();
        try {
            ItemSource item = jdbcDao.getItemSource(itemId);//itemSourceDao.findUniqueBy("itemId", itemId);
            DbResource db = jdbcDao.getDbResource(item.getDbId());//.findUniqueBy("id", item.getDbId());

            resultList = execSp2(item, db, map);
        } catch (Exception e) {
            logger.error("execSp:", e);
        }
        return resultList;
    }

    @SuppressWarnings("rawtypes")
    public static List<Map> execSp2(ItemSource item, DbResource db, Map<Integer, Object> map) throws Exception {

        Connection conn = null;
        CallableStatement cs = null;// PreparedStatement,Statement
        ResultSet rs = null;
        if (item.getDbId() == null)
            return null;
        List<Map> resultList = new ArrayList<Map>();
        try {

            if (db.getDbtype().equals(MobConstants.DBTYPE_MSSQL)) {
                conn = JDBCUtil.getMsSqlConnection(db.getUrl(), db.getDbuser(), db.getPasswd());
            } else if (db.getDbtype().equals(MobConstants.DBTYPE_ORACLE)) {
                conn = JDBCUtil.getOracleConnection(db.getUrl(), db.getDbuser(), db.getPasswd());
            } else if (db.getDbtype().equals(MobConstants.DBTYPE_CACHE)) {
                conn = JDBCUtil.getCacheConnection(db.getUrl(), db.getDbuser(), db.getPasswd());
            }

            StringBuffer storedProcName = new StringBuffer("{call ");
            storedProcName.append(item.getSpName());
            storedProcName.append("(");

            if (db.getDbtype().equals(MobConstants.DBTYPE_ORACLE)) {
                // 默认有个返回结果
                for (int i = 0; i < map.size(); i++) {
                    storedProcName.append("?");
                    storedProcName.append(", ");
                }
                storedProcName.append("?");
                storedProcName.append(")}");

                cs = conn.prepareCall(storedProcName.toString());
                for (int j = 1; j <= map.size(); j++) {
                    cs.setObject(j, map.get(j));
                }
                cs.registerOutParameter(map.size() + 1, OracleTypes.CURSOR);
            } else {
                StringBuilder sqlLike = new StringBuilder();
                for (int i = 0; i < map.size(); i++) {
                    if (sqlLike.length() > 0)
                        sqlLike.append(",");
                    sqlLike.append("?");
                }
                sqlLike.append(")}");
                cs = conn.prepareCall(storedProcName.toString() + sqlLike.toString());
                for (int j = 1; j <= map.size(); j++) {
                    cs.setObject(j, map.get(j));
                }
            }

            if (db.getDbtype().equals(MobConstants.DBTYPE_ORACLE)) {
                cs.execute();
                if (!item.getSpName().contains("commit"))
                    rs = (ResultSet) cs.getObject(map.size() + 1);
            } else {
                rs = cs.executeQuery();
            }
            if (!item.getSpName().contains("commit")) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                Map<String, Object> rowMap = null;
                String headName = "";
                while (rs.next()) {
                    rowMap = new HashMap<String, Object>();
                    for (int i = 1; i <= cols; i++) {
                        headName = meta.getColumnName(i);

                        if (rs.getObject(headName) != null) {
                            rowMap.put(headName, rs.getString(headName));
                        } else {
                            rowMap.put(headName, "");
                        }
                    }
                    resultList.add(rowMap);
                }
            }

        } catch (Exception e) {
            logger.error(item.getItemName() + "执行失败:", e);
            throw new Exception(e);
        } finally { // 防止连接泄露
            try {
                if (cs != null)
                    cs.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    String spellDate(String OriginalDate) {
        String DDate = OriginalDate.trim().substring(0, 10);
        String Year = DDate.substring(0, 4);
        String month = DDate.substring(5, 7);
        String day = DDate.substring(8, 10);
        String retDate = Year + "-" + month + "-" + day;
        return retDate;
    }

    public void setJdbcDao(JdbcDao jdbcDao) {
        this.jdbcDao = jdbcDao;
    }

    public void setmDataDao(MDataDao mDataDao) {
        this.mDataDao = mDataDao;
    }
}
