package com.mengnankk.config;

import lombok.Data;

@Data
public class ChatSessionConfig {
    private String model;
    private float temperature = 0.7f;
    private int maxTokens = 2000;
    private boolean stream = false;
}
