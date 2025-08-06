package com.mengnankk.entity.ai;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMcpTool implements Mcptool{
    protected final String name;
    protected final String description;
    protected final Map<String, Object> parameterSchema;

    public AbstractMcpTool(String name, String description) {
        this.name = name;
        this.description = description;
        this.parameterSchema = new HashMap<>();
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        return null;
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return parameterSchema;
    }
    protected abstract void initParameterSchema();
    protected void validateParameters(Map<String, Object> parameters){

    }
}
