package com.mengnankk.entity.ai;

import java.util.Map;

public interface Mcptool {
    String getName();

    String getDescription();
    Object execute(Map<String, Object> parameters);
    Map<String, Object> getParameterSchema();
}
