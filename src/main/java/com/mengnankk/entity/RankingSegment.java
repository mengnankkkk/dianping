package com.mengnankk.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class RankingSegment {
    private String segmentName;          // 分段名称，如 "Top 100"
    private double minRank;               // 起始排名
    private double maxRank;                 // 结束排名
    private long totalCount;             // 该榜单总成员数
    private String category;             // 榜单类别
    private String timeType;             // 时间维度，如 "week"
    private List<RankingItem> rankingItems;  // 当前分段排行项列表
}
