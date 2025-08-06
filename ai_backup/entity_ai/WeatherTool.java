package com.mengnankk.entity.ai;

import java.util.HashMap;
import java.util.Map;

public class WeatherTool extends AbstractMcpTool {

    public WeatherTool() {
        super("weather", "获取指定城市的天气信息");
    }

    @Override
    protected void initParameterSchema() {
        Map<String, Object> cityParam = new HashMap<>();
        cityParam.put("type", "string");
        cityParam.put("description", "城市名称");
        cityParam.put("required", true);

        parameterSchema.put("city", cityParam);
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        validateParameters(parameters);

        String city = (String) parameters.get("city");

        // 模拟天气查询
        Map<String, Object> result = new HashMap<>();
        result.put("city", city);
        result.put("temperature", "25°C");
        result.put("weather", "晴天");
        result.put("humidity", "60%");

        return result;
    }
}