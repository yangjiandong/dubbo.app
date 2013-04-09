package org.ssh.dubbo.demo.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.ssh.dubbo.demo.utils.JDBCUtil;

import com.ek.mobileapp.dubbo.DbResource;
import com.ek.mobileapp.dubbo.ItemSource;

public class JdbcDao {
    static Logger logger = LoggerFactory.getLogger(JdbcDao.class);
    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private ItemSourceMapper itemSourceMapper = new ItemSourceMapper();

    private class ItemSourceMapper implements RowMapper<ItemSource> {
        public ItemSource mapRow(ResultSet rs, int rowNum) throws SQLException {
            ItemSource itemSource = new ItemSource();
            itemSource.setId(rs.getLong("id"));
            itemSource.setItemId(rs.getLong("itemId"));
            itemSource.setItemName(rs.getString("itemName"));
            itemSource.setSpName(rs.getString("spName"));
            itemSource.setTypeCode(rs.getString("typeCode"));
            itemSource.setCronId(rs.getLong("cronId"));
            itemSource.setCron(rs.getString("cron"));
            itemSource.setExpression(rs.getString("expression"));
            itemSource.setDbId(rs.getLong("dbId"));
            itemSource.setIsAuto(rs.getString("isAuto"));
            itemSource.setDbResource(rs.getString("dbResource"));
            return itemSource;
        }
    }

    private DbResourceMapper dbResourceMapper = new DbResourceMapper();

    private class DbResourceMapper implements RowMapper<DbResource> {
        public DbResource mapRow(ResultSet rs, int rowNum) throws SQLException {
            DbResource dbReSource = new DbResource();
            dbReSource.setId(rs.getLong("id"));
            dbReSource.setDescript(rs.getString("descript"));
            dbReSource.setDbtype(rs.getString("dbtype"));
            dbReSource.setDbuser(rs.getString("dbuser"));
            dbReSource.setPasswd(rs.getString("passwd"));
            dbReSource.setUrl(rs.getString("url"));
            return dbReSource;
        }
    }

    public static String nowDateString(String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /**
     * 获取服务器端的指定格式的当前时间字符串,oracle数据库的时间格式为"yyyy.MM.dd HH24:mi:ss"
     */
    public String getNowString(String format) {
        String sdate = nowDateString("yyyy.MM.dd HH:mm:ss");
        if (format.length() == 10) {
            sdate = nowDateString("yyyy.MM.dd");
        }

        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection con = null;
        try {
            con = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            String sql = "";
            //2011.05.02
            //连接泄露
            //(DBUtils.isOracle(jdbcTemplate2.getDataSource().getConnection()))
            //if (DBUtils.isOracle(con)) {
            if (JDBCUtil.isOracle(con)) {
                sql = "select to_char(sysdate,'yyyy-MM-dd HH24:mi:ss') as sys_date from dual";
            } else if (JDBCUtil.isMSSqlServer(con)) {
                sql = "Select CONVERT(varchar(100), GETDATE(), 120)";
            } else {
                sql = ""; // 其他数据库，则采用应用服务器系统时间
            }

            if (StringUtils.isNotEmpty(sql)) {
                Object result = this.jdbcTemplate.queryForObject(sql, null, String.class);
                if (result != null) {
                    sdate = result.toString();
                }

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = formatter.parse(sdate);
                sdate = new SimpleDateFormat(format).format(date);
            }

        } catch (Exception se) {
            logger.error("getNowString:", se);
        } finally {
            //安全释放
            DataSourceUtils.releaseConnection(con, dataSource);
        }

        return sdate;
    }

    public ItemSource getItemSource(Long itemId) {
        return this.jdbcTemplate.queryForObject("SELECT * FROM Mob_ItemSource WHERE itemId = ?", itemSourceMapper,
                itemId);
    }

    public DbResource getDbResource(Long dbId) {
        return this.jdbcTemplate.queryForObject("SELECT * FROM Mob_DbResource WHERE id = ?", dbResourceMapper, dbId);
    }

}
