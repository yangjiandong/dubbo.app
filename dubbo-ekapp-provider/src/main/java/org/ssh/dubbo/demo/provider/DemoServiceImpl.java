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

import com.alibaba.dubbo.rpc.RpcContext;

public class DemoServiceImpl implements DemoService<Hz> {
    private JdbcTemplate jdbcTemplate;

    private UserMapper userMapper = new UserMapper();

    private class UserMapper implements RowMapper<Hz> {
        public Hz mapRow(ResultSet rs, int rowNum) throws SQLException {
            Hz user = new Hz();
            user.setId(rs.getLong("id"));
            user.setHz(rs.getString("hz"));
            user.setWb(rs.getString("wb"));
            user.setPy(rs.getString("py"));
            return user;
        }
    }

    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name
                + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }

    public List<Hz> getPageItems(int page) {
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