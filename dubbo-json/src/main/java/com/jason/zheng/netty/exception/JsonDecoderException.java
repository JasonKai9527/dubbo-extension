package com.jason.zheng.netty.exception;

import io.netty.handler.codec.DecoderException;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 13:57 2021/6/30
 */
public class JsonDecoderException extends DecoderException {
    private static final long serialVersionUID = -668780793648449671L;

    public JsonDecoderException() {}

    public JsonDecoderException(String message, Throwable cause) {
        super(message, cause);
    }
    public JsonDecoderException(String message) {
        super(message);
    }
    public JsonDecoderException(Throwable cause) {
        super(cause);
    }
}
