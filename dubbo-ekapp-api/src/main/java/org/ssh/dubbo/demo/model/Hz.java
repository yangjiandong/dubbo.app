package org.ssh.dubbo.demo.model;

import java.io.Serializable;

public class Hz implements Serializable {
    private static final long serialVersionUID = -6269235058613434794L;

    private Long id;
    private String hz;
    private String wb;
    private String py;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHz() {
        return hz;
    }

    public void setHz(String hz) {
        this.hz = hz;
    }

    public String getWb() {
        return wb;
    }

    public void setWb(String wb) {
        this.wb = wb;
    }

    public String getPy() {
        return py;
    }

    public void setPy(String py) {
        this.py = py;
    }

    @Override
    public String toString() {
        return getHz();
    }
}
