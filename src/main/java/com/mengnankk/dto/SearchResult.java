package com.mengnankk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResult<T> {
    private List<T> results;           // 搜索结果列表
    private Object[] nextSearchAfter;  // ES 分页游标
    private Integer total;             // 结果总数
    private String query;              // 查询词
    private Long searchTime;           // 查询耗时或时间戳
    private boolean fallback;          // 是否回退（如召回失败，使用默认结果）
    private String error;              // 错误信息（如服务不可用等）
}
