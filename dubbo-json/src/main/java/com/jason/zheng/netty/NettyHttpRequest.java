package com.jason.zheng.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.ObjectUtil;

import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * @Description:
 * @Author: psbc
 * @Date: 15:43 2021/6/29
 */
public class NettyHttpRequest implements HttpRequest {

    private final ChannelHandlerContext ctx;
    private HttpMethod httpMethod;
    private InputStream inputStream;
    private final boolean is100ContinueExpected;
    private HttpHeaders httpHeaders;
    private String uri;
    private HttpVersion httpVersion;
    private DecoderResult decoderResult = DecoderResult.SUCCESS;

    public NettyHttpRequest(ChannelHandlerContext ctx, HttpMethod httpMethod, boolean is100ContinueExpected,
                            HttpHeaders httpHeaders, HttpVersion version, String uri) {
        this.ctx = ctx;
        this.httpMethod = httpMethod;
        this.is100ContinueExpected = is100ContinueExpected;
        this.httpHeaders = httpHeaders;
        this.httpVersion = version;
        this.uri = uri;
    }
    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) ctx.channel().localAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) ctx.channel().remoteAddress();
    }

    public boolean is100ContinueExpected() {
        return is100ContinueExpected;
    }

    @Override
    public HttpMethod getMethod() {
        return httpMethod;
    }

    @Override
    public HttpMethod method() {
        return getMethod();
    }

    @Override
    public HttpRequest setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String uri() {
        return getUri();
    }

    @Override
    public HttpRequest setUri(String s) {
        ObjectUtil.checkNotNull(s, "uri");
        this.uri = s;
        return this;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return httpVersion;
    }

    @Override
    public HttpVersion protocolVersion() {
        return httpVersion;
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion httpVersion) {
        ObjectUtil.checkNotNull(httpVersion, "httpVersion");
        this.httpVersion = httpVersion;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return httpHeaders;
    }


    @Override
    public DecoderResult getDecoderResult() {
        return decoderResult;
    }

    @Override
    public DecoderResult decoderResult() {
        return getDecoderResult();
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        this.decoderResult = decoderResult;
    }
}
