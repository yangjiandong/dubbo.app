package org.ssh.dubbo.demo.provider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.ssh.dubbo.demo.DemoService;
import org.ssh.dubbo.demo.model.Hz;
import org.ssh.dubbo.demo.utils.JdbcPaginationHelper;
import org.ssh.dubbo.demo.utils.SpringContextHolder;
import org.ssh.pm.SysConfigData;

import com.alibaba.dubbo.rpc.RpcContext;

//@Service(version="1.0.0")
public class DemoServiceImpl implements DemoService<Hz> {
    private JdbcTemplate jdbcTemplate;

    private UserMapper userMapper = new UserMapper();

    private class UserMapper implements RowMapper<Hz> {
        public Hz mapRow(ResultSet rs, int rowNum) throws SQLException {
            Hz hz = new Hz();
            hz.setId(rs.getLong("id"));
            hz.setHz(rs.getString("hz"));
            hz.setWb(rs.getString("wb"));
            hz.setPy(rs.getString("py"));
            return hz;
        }
    }

    public String sayHello(String name) {
        SysConfigData sys = (SysConfigData)SpringContextHolder.getBean("sysConfigData");

        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name
                + ", request from consumer: " + RpcContext.getContext().getRemoteAddress()
                + ", provider version: " + sys.getVersion());
        return "Hello " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }

    public List<Hz> getPageItems(int page) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] getPageItems " + page
                + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());

        org.ssh.dubbo.demo.utils.JdbcPaginationHelper<Hz> ph = new JdbcPaginationHelper<Hz>();
        return ph.fetchPage(jdbcTemplate, "SELECT count(*) FROM t_hzk ", "SELECT id,hz,py,wb FROM t_hzk",
                null, page, 20, userMapper).getPageItems();

    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}