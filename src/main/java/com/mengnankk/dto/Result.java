package com.mengnankk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {  //  添加类型参数 <T>
    private boolean success;
    private T data;  //  使用类型参数
    private String errorMsg;

    public static <T> Result<T> ok(T data) {  // 添加泛型方法，保证不同static方法返回值一致
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        result.setErrorMsg(null);
        return result;
    }

    public static <T> Result<T> ok() {  // 添加泛型方法，保证不同static方法返回值一致
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(null);
        result.setErrorMsg(null);
        return result;
    }

    public static <T> Result<T> fail(String errorMsg) {  // 添加泛型方法，保证不同static方法返回值一致
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setData(null);
        result.setErrorMsg(errorMsg);
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
}
