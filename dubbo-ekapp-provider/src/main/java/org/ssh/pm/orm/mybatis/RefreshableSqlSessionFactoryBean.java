package org.ssh.pm.orm.mybatis;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;

/**
 * mybatis reload xml
 *
 * http://sbcoba.tistory.com/entry/Spring-mybats-%EC%82%AC%EC%9A%A9%EC%8B%9C-%EC%9E%AC%EC%8B%9C%EC%9E%91-%EC%97%86%EC%9D%B4-%EC%84%9C%EB%B2%84-%EB%B0%98%EC%98%81 *
 */

// <bean id="sqlSessionFactory" class=".RefreshableSqlSessionFactoryBean"
//    p:mapperLocations="classpath*:kr/web/**/mapper.xml"
//    p:configLocation="classpath:/MapperConfig.xml"
//    p:dataSource-ref="dataSource" p:interval="1000" />

public class RefreshableSqlSessionFactoryBean extends SqlSessionFactoryBean implements DisposableBean {
    private static final Log log = LogFactory.getLog(RefreshableSqlSessionFactoryBean.class);
    private SqlSessionFactory proxy;
    private int interval = 500;
    private Timer timer;
    private TimerTask task;
    private Resource[] mapperLocations;

    private boolean running = false;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    public void setMapperLocations(Resource[] mapperLocations) {
        super.setMapperLocations(mapperLocations);
        this.mapperLocations = mapperLocations;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     *
     * @throws Exception
     */
    public void refresh() throws Exception {
        if (log.isInfoEnabled()) {
            log.info("refreshing sqlMapClient.");
        }

        w.lock();
        try {
            super.afterPropertiesSet();
        } finally {
            w.unlock();
        }

    }


    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        setRefreshable();
    }

    private void setRefreshable() {
        proxy = (SqlSessionFactory) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[] { SqlSessionFactory.class },
        new InvocationHandler() {
            public Object invoke(Object proxy, Method method,
            Object[] args) throws Throwable {
                // log.debug("method.getName() : " + method.getName());
                return method.invoke(getParentObject(), args);
            }
        });

        task = new TimerTask() {
            private Map<Resource, Long> map = new HashMap<Resource, Long>();
            public void run() {
                if (isModified()) {
                    try {
                        refresh();
                    } catch (Exception e) {
                        log.error("caught exception", e);
                    }
                }
            }

            private boolean isModified() {
                boolean retVal = false;
                if (mapperLocations != null) {
                    for (int i = 0; i < mapperLocations.length; i++) {
                        Resource mappingLocation = mapperLocations[i];
                        retVal |= findModifiedResource(mappingLocation);
                    }
                }
                return retVal;
            }

            //检查资源文件是否有更改
            private boolean findModifiedResource(Resource resource) {
                boolean retVal = false;
                List<String> modifiedResources = new ArrayList<String>();
                try {
                    long modified = resource.lastModified();
                    if (map.containsKey(resource)) {
                        long lastModified = ((Long) map.get(resource))
                        .longValue();

                        if (lastModified != modified) {
                            map.put(resource, new Long(modified));
                            modifiedResources.add(resource.getDescription());
                            retVal = true;
                        }

                    } else {
                        map.put(resource, new Long(modified));
                    }
                } catch (IOException e) {
                    log.error("caught exception", e);
                }

                if (retVal) {
                    if (log.isInfoEnabled()) {
                        log.info("modified files : " + modifiedResources);
                    }
                }
                return retVal;
            }
        };

        timer = new Timer(true);
        resetInterval();
    }

    private Object getParentObject() throws Exception {
        r.lock();
        try {
            return super.getObject();
        } finally {
            r.unlock();
        }

    }

    public SqlSessionFactory getObject() {
        return this.proxy;
    }

    public Class<? extends SqlSessionFactory> getObjectType() {
        return (this.proxy != null ? this.proxy.getClass()
        : SqlSessionFactory.class);
    }

    public boolean isSingleton() {
        return true;
    }

    public void setCheckInterval(int ms) {
        interval = ms;
        if (timer != null) {
            resetInterval();
        }
    }

    private void resetInterval() {
        if (running) {
            timer.cancel();
            running = false;
        }

        if (interval > 0) {
            timer.schedule(task, 0, interval);
            running = true;
        }
    }

    public void destroy() throws Exception {
        timer.cancel();
    }
}