package com.luoboduner.moo.tool.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户自定义功能分组。
 */
public class FuncGroup {

    private String name;

    private List<String> funcIds = new ArrayList<>();

    public FuncGroup() {
    }

    public FuncGroup(String name, List<String> funcIds) {
        this.name = name;
        this.funcIds = funcIds != null ? new ArrayList<>(funcIds) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFuncIds() {
        return funcIds;
    }

    public void setFuncIds(List<String> funcIds) {
        this.funcIds = funcIds != null ? new ArrayList<>(funcIds) : new ArrayList<>();
    }
}
