package com.jason.zheng.netty;

import com.jason.zheng.netty.exception.JsonDecoderException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.RequestingUserName;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 16:05 2021/7/1
 */
public class RequestHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private NettyHttpHandler handler;
    public RequestHandler(NettyHttpHandler httpHandler) {
        this.handler = httpHandler;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if (o instanceof NettyHttpRequest) {
            NettyHttpRequest request = (NettyHttpRequest) o;
            if (request.is100ContinueExpected()) {
                send100Continue(channelHandlerContext);
            }
            NettyHttpResponse response = new NettyHttpResponse(channelHandlerContext, HttpUtil.isKeepAlive(request), request.getProtocolVersion());
            try {
                handler.handle(request, response);
            } catch (Exception e) {
                response.reset();
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                logger.error("Unexpected", e);
            }
            response.finish();
        }
    }

    private void send100Continue(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        if (e.getCause() instanceof TooLongFrameException) {
            DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            ctx.write(defaultHttpResponse).addListener(ChannelFutureListener.CLOSE);
        } else if (e.getCause() instanceof JsonDecoderException) {
            NettyHttpResponse response = new NettyHttpResponse(ctx);
            response.sendError(HttpResponseStatus.BAD_REQUEST);
        } else {
            logger.error("channel handler invoke failed.", e);
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.close();
        }
    }
}
