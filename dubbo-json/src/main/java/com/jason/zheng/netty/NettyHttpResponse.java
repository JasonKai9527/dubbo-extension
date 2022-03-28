package com.jason.zheng.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.ObjectUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CheckedOutputStream;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 14:08 2021/6/30
 */
public class NettyHttpResponse implements HttpResponse {
    private static final int EMPTY_CONTENT_LENGTH = 0;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private OutputStream outputStream;
    private final ChannelHandlerContext ctx;
    private boolean keepAlive;
    private boolean committed;
    private HttpHeaders headers;
    private HttpVersion httpVersion;
    private DecoderResult decoderResult = DecoderResult.SUCCESS;

    public NettyHttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
    public NettyHttpResponse(ChannelHandlerContext ctx, boolean keepAlive, HttpVersion version) {
        this.ctx = ctx;
        this.keepAlive = keepAlive;
        this.outputStream = new ChunkOutputStream(this, ctx, 1000);
    }

    public void finish() throws IOException {
        outputStream.flush();
        ChannelFuture future;
        if (isCommitted()) {
            future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            future = ctx.writeAndFlush(getEmptyHttpResponse());
        }
        if (isKeepAlive()) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void sendError(HttpResponseStatus status) {
        sendError(status, null);
    }
    public void sendError(HttpResponseStatus status, String message) {
        if (committed) {
            throw new IllegalStateException("already committed");
        }
        HttpResponse response;
        if (message != null) {
            ByteBuf byteBuf = ctx.alloc().buffer();
            byteBuf.writeBytes(message.getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
        } else {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        }
        if (keepAlive) {
            response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            if (message == null) {
                response.headers().add(CONTENT_LENGTH, 0);
            } else {
                response.headers().add(CONTENT_LENGTH, message.getBytes().length);
            }
        }
        ctx.writeAndFlush(response);
        committed = true;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public DefaultHttpResponse getEmptyHttpResponse() {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, getStatus());
        response.headers().add(CONTENT_LENGTH, EMPTY_CONTENT_LENGTH);
        transformResponseHeaders(response);
        return response;

    }
    public DefaultHttpResponse getDefaultHttpResponse() {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, getStatus());
        transformResponseHeaders(response);
        return response;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private void transformResponseHeaders(HttpResponse response) {
        JsonHttpResponseEncoder.transformHeaders(this, response);
    }

    public boolean isCommitted() {
        return committed;
    }

    public void prepareChunkSteam() {
        committed = true;
        DefaultHttpResponse response = getDefaultHttpResponse();
        HttpUtil.setTransferEncodingChunked(response, true);
        ctx.write(response);
    }

    public void reset() {
        if (committed) {
            throw new IllegalStateException("Already committed");
        }
        headers.clear();
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(String key, Object value) {
        headers.set(key, value);
    }

    @Override
    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public HttpResponse setStatus(HttpResponseStatus httpResponseStatus) {
        this.status = ObjectUtil.checkNotNull(status, "status");
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
    public HttpResponse setProtocolVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
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
