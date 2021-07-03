package com.jason.zheng.json;

import com.jason.zheng.netty.NettyHttpHandler;
import org.apache.dubbo.common.URL;

import javax.net.ssl.SSLContext;

/**
 * @Description:
 * @Author: psbc
 * @Date: 15:34 2021/6/29
 */
public interface HttpServer {
    void start();

    URL getUrl();

    void setUrl(URL url);

    void setHandler(NettyHttpHandler handler);

    void setSslContext(SSLContext sslContext);

    void close();
}
