package com.jason.zheng.json;

import com.jason.zheng.netty.NettyHttpServer;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 15:33 2021/6/29
 */
public class ServerFactory {
    public HttpServer createServer(String name) {
        if ("netty".equalsIgnoreCase(name)) {
            return new NettyHttpServer();
        } else {
            throw new IllegalArgumentException("Unrecognized server name: " + name);
        }
    }
}
