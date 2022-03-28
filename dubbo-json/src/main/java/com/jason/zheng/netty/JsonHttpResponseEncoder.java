package com.jason.zheng.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.List;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 14:07 2021/6/30
 */
public class JsonHttpResponseEncoder extends MessageToMessageEncoder<NettyHttpResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, NettyHttpResponse nettyHttpResponse, List<Object> out) throws Exception {
        nettyHttpResponse.getOutputStream().flush();
        if (nettyHttpResponse.isCommitted()) {
            out.add(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            HttpResponse response = nettyHttpResponse.getDefaultHttpResponse();
            out.add(response);
        }
    }

    public static void transformHeaders(NettyHttpResponse nettyHttpResponse, HttpResponse response) {
        HttpHeaders headers = nettyHttpResponse.headers();
        if (headers != null) {
            response.headers().add(nettyHttpResponse.headers());
        }
        if (nettyHttpResponse.isKeepAlive()) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        } else {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        }
    }
}
