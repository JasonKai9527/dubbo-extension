package com.jason.zheng.netty;

import com.jason.zheng.json.HttpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NetUtil;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.remoting.Constants.*;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 15:38 2021/6/29
 */
public class NettyHttpServer implements HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private SSLContext sslContext;
    private URL url;
    private NettyHttpHandler handler;
    private ServerBootstrap bootstrap;
    private int maxInitialLineLength = 4096;
    private int maxHeaderSize = 8192;
    private int maxChunkSize = 8192;
    private int idleTimeout = -1;
    public NettyHttpServer() {

    }

    @Override
    public void start() {
        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyHttpServerBoss");
        NettyEventLoopFactory.eventLoopGroup(url.getPositiveParameter(IO_THREADS_KEY, DEFAULT_IO_THREADS),
                "NettyHttpServerWorker");

        bootstrap = new ServerBootstrap();
        int idleTimeout = url.getParameter(IDLE_TIMEOUT_KEY, DEFAULT_IDLE_TIMEOUT);
        bootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childHandler(createChannelInitializer());
        String bindIp = url.getParameter(BIND_IP_KEY, url.getHost());
        int bindPort = url.getParameter(BIND_PORT_KEY, url.getPort());
        if (url.getParameter(ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
            bindIp = "0.0.0.0";
        }
        InetSocketAddress bindAddress = new InetSocketAddress(bindIp, bindPort);
        ChannelFuture channelFuture = bootstrap.bind(bindAddress);
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }
    private ChannelInitializer<SocketChannel> createChannelInitializer() {
        if (sslContext == null) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    setHandler(socketChannel, handler);
                }
            };
        } else {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new SslHandler(engine));
                    setHandler(socketChannel, handler);
                }
            };
        }
    }

    private void setHandler(SocketChannel channel, NettyHttpHandler httpHandler) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
        pipeline.addLast(new HttpObjectAggregator(url.getParameter(PAYLOAD_KEY, DEFAULT_PAYLOAD)));
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new JsonHttpRequestDecoder());
        pipeline.addLast(new JsonHttpResponseEncoder());
        if (idleTimeout > 0) {
            pipeline.addLast("nettyHttpServer-idleHandler", new IdleStateHandler(0, 0, idleTimeout, TimeUnit.MILLISECONDS));
        }
        pipeline.addLast(new RequestHandler(httpHandler));
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public void setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public NettyHttpHandler getHandler() {
        return handler;
    }

    public void setHandler(NettyHttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
