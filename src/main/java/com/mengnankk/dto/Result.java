package com.mengnankk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private boolean success;
    private Object data;
    private String errorMsg;
    public static Result ok(Object data){
        return new Result(true,data,null);
    }
    public static Result ok(){
        return new Result(true,null,null);
    }
    public static Result fail(String errorMsg){
        return new Result(false,null,errorMsg);
    }
}
