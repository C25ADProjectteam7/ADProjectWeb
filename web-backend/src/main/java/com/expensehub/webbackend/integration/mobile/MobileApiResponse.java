package com.expensehub.webbackend.integration.mobile;

/**
 * Response packet format corresponding to the Mobile group com.team7.mobile.common.dto.ApiResponse:
 * { "code": 200, "message": "success", "data": ... }
 * All interfaces of Mobile are in this shape, and they are all unpacked here after being called from the Web side.
 */
public class MobileApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public MobileApiResponse() {}

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return code == 200;
    }
}
