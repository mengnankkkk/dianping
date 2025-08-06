package com.mengnankk.service.ai;


import com.mengnankk.dto.Result;
import com.mengnankk.entity.ai.Mcptool;
import com.mengnankk.entity.ai.WeatherTool;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpService {
    private final ChatModel chatModel;
    private final Map<String , Mcptool> tools  = new ConcurrentHashMap();

    @PostConstruct
    public void initializeTools() {
        registerTool(new WeatherTool());

        log.info("Initialized {} MCP tools\", tools.size()");
    }

    private void registerTool(Mcptool tool){
        tools.put(tool.getName(), tool);
        log.info("Registered MCP tool: {}", tool.getName());
    }

    /**
     * 调用mcp工具
     * @param toolname
     * @param parameters
     * @return
     */
    public Result executeTool(String toolname,Map<String,Object> parameters){
        Mcptool tool = tools.get(toolname);
        if (tool==null){
            return Result.fail("Not found");
        }
        try {

            Object result = tool.execute(parameters);
            return Result.ok(result);
        }catch (Exception e){
            log.error("Error executing tool {}: {}", toolname, e.getMessage(), e);
            return Result.ok("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 搜索mcp
     * @return
     */
    public List<Mcptool> getAvailableTools(){
        return tools.values().stream()
                .collect(Collectors.toList());
        //TODO 可以改成可用的MCP
    }
    public Result  smartToolCall(String userInput){
        return null;
    }//TODO 完成智能调用
}
