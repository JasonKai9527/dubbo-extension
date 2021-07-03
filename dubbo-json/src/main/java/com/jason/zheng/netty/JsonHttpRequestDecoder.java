package com.jason.zheng.netty;

import com.jason.zheng.netty.exception.JsonDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

/**
 * @Description:
 * @Author: psbc
 * @Date: 11:20 2021/6/30
 */
@ChannelHandler.Sharable
public class JsonHttpRequestDecoder extends MessageToMessageDecoder<HttpRequest> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, HttpRequest httpRequest, List<Object> list) throws Exception {
        final HttpHeaders headers = httpRequest.headers();
        final String uri = httpRequest.uri();
        final HttpVersion httpVersion = httpRequest.protocolVersion();
        final NettyHttpRequest request = new NettyHttpRequest(channelHandlerContext, httpRequest.method(), is100ContinueExpected(httpRequest)
                , headers, httpVersion, uri);
        try {
            if (httpRequest instanceof HttpContent) {
                HttpContent content = (HttpContent) httpRequest;
                if (content.content().readableBytes() > 0) {
                    ByteBuf byteBuf = content.content().retain();
                    ByteBufInputStream inputStream = new ByteBufInputStream(byteBuf);
                    request.setInputStream(inputStream);
                }
                list.add(request);
            }
        } catch (Exception e) {
            request.setDecoderResult(DecoderResult.failure(e));
            throw new JsonDecoderException("failed to parse httpRequest. uri = " + httpRequest.uri(), e);
        }
    }
}
