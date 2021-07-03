package com.jason.zheng.json;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import com.jason.zheng.netty.NettyHttpHandler;
import com.jason.zheng.netty.NettyHttpRequest;
import com.jason.zheng.netty.NettyHttpResponse;
import com.sun.org.apache.regexp.internal.RE;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.dubbo.rpc.protocol.http.JsonRemoteInvocation;
import org.apache.dubbo.rpc.protocol.http.JsonRpcProxyFactoryBean;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * @Description:
 * @Author: psbc
 * @Date: 14:03 2021/6/29
 */
public class JsonProtocol extends AbstractProxyProtocol {

    public static final Logger logger = LoggerFactory.getLogger(JsonProtocol.class);
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<>();
    private final Map<String, JsonRpcServer> skeletonMap = new ConcurrentHashMap<>();

    private final ServerFactory serverFactory = new ServerFactory();

    private class InternalHandler implements NettyHttpHandler {

        private boolean cors;

        public InternalHandler(boolean cors) {
            this.cors = cors;
        }

        @Override
        public void handle(NettyHttpRequest httpRequest, NettyHttpResponse httpResponse) throws Exception {
            String uri = httpRequest.getUri();
            JsonRpcServer skeleton = skeletonMap.get(uri);
            if (cors) {
                httpResponse.setHeaders(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                httpResponse.setHeaders(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST");
                httpResponse.setHeaders(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "*");
            }
            if (httpRequest.getMethod().name().equalsIgnoreCase("OPTIONS")) {
                httpResponse.setStatus(HttpResponseStatus.OK);
            } else if (httpRequest.getMethod().name().equalsIgnoreCase("POST")) {
                RpcContext.getContext().setRemoteAddress(httpRequest.getRemoteAddress());
                try {
                    skeleton.handle(httpRequest.getInputStream(), httpResponse.getOutputStream());
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            } else {
                httpResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        HttpServer httpServer = serverMap.get(addr);
        if (httpServer == null) {
            synchronized (this) {
                httpServer = serverMap.get(addr);
                if (httpServer == null) {
                    HttpServer server = serverFactory.createServer("netty");
                    server.setUrl(url);
                    server.setHandler(new InternalHandler(url.getParameter("cors", false)));
                    server.start();
                    serverMap.put(addr, server);
                }
            }
        }
        String path = url.getAbsolutePath();
        String genericPath = path + "/" + GENERIC_KEY;
        JsonRpcServer skeleton = new JsonRpcServer(impl, type);
        JsonRpcServer genericServer = new JsonRpcServer(impl, GenericService.class);
        skeletonMap.put(path, skeleton);
        skeletonMap.put(genericPath, genericServer);

        return () -> {
            skeletonMap.remove(path);
            skeletonMap.remove(genericPath);
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        String generic = url.getParameter(GENERIC_KEY);
        boolean isGeneric = ProtocolUtils.isGeneric(generic) || type.equals(GenericService.class);
        JsonProxyFactoryBean proxyFactoryBean = new JsonProxyFactoryBean();
        JsonRpcProxyFactoryBean jsonRpcProxyFactoryBean = new JsonRpcProxyFactoryBean(proxyFactoryBean);
        jsonRpcProxyFactoryBean.setRemoteInvocationFactory(methodInvocation -> {
            JsonRemoteInvocation invocation = new JsonRemoteInvocation(methodInvocation);
            if (isGeneric) {
                invocation.addAttribute(GENERIC_KEY, generic);
            }
            return invocation;
        });
        String key = url.setProtocol("http").toIdentityString();
        if (isGeneric) {
            key = key + "/" + GENERIC_KEY;
        }
        jsonRpcProxyFactoryBean.setServiceUrl(key);
        jsonRpcProxyFactoryBean.setServiceInterface(type);
        proxyFactoryBean.afterPropertiesSet();
        return (T) jsonRpcProxyFactoryBean.getObject();
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    logger.info("Close netty server {}", server.getUrl());
                    server.close();
                } catch (Throwable throwable) {
                    logger.warn(throwable.getMessage(), throwable);
                }
            }
        }
    }

    protected  int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<? extends Throwable> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    @Override
    public int getDefaultPort() {
        return 0;
    }
}
