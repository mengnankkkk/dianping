package com.mengnankk.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;            // 用户 ID
    private String nickname;    // 用户昵称
    private String icon;        // 用户头像 URL
}
