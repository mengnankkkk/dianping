package com.mengnankk.entity;

import lombok.Builder;
import lombok.Data;

import javax.xml.stream.Location;
import java.util.List;

@Data
@Builder
public class UserIntent {
    private IntentType type;
    private String cuisine;
    private String occasion;
    private Location location;
    private Double distance;
    private List<String> keywords;
    private Double confidence;

    public enum IntentType {
        NEARBY_SEARCH,
        CUISINE_RECOMMENDATION,
        PRICE_BASED,
        OCCASION_BASED,
        GENERAL
    }
}