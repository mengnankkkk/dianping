package com.mengnankk.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 滚动分页结果DTO
 * 用于分页滚动查询的结果封装
 */
@Data
@Builder
public class ScrollResult {
    private List<?> list;           // 数据列表
    private Long minTime;           // 最小时间（用于下次查询）
    private Integer offset;         // 偏移量
    private boolean hasMore;        // 是否还有更多数据
}
