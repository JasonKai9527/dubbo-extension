package com.jason.zheng.netty;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 15:43 2021/6/29
 */
public interface NettyHttpHandler {
    void handle(NettyHttpRequest httpRequest, NettyHttpResponse httpResponse) throws Exception;
}
