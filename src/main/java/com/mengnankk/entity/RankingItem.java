package com.mengnankk.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankingItem {
    private Long member;   // 成员ID或名称
    private double score;    // 分数
    private Integer rank;    // 排名（可选）

    public RankingItem() {
    }

    public RankingItem(Long member, double score, Integer rank) {
        this.member = member;
        this.score = score;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "RankingItem{" +
                "member='" + member + '\'' +
                ", score=" + score +
                ", rank=" + rank +
                '}';
    }
}
