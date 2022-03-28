package com.jason.zheng.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @Description:
 * @Author: Kai Zheng
 * @Date: 14:18 2021/6/30
 */
public class ChunkOutputStream extends OutputStream {
    final ByteBuf byteBuf;
    final ChannelHandlerContext ctx;
    final NettyHttpResponse response;

    ChunkOutputStream(NettyHttpResponse response, ChannelHandlerContext ctx, int chunkSize) {
        this.response = response;
        this.ctx = ctx;
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be at least 1");
        }
        this.byteBuf = Unpooled.buffer(0, chunkSize);
    }
    @Override
    public void write(int b) throws IOException {
        if (byteBuf.maxWritableBytes() < 1) {
            flush();
        }
        byteBuf.writeByte(b);
    }
    public void reset() {
        if (response.isCommitted()) {
            throw new IllegalStateException("response is committed");
        }
        byteBuf.clear();
    }

    @Override
    public void flush() throws IOException {
        int readableBytes = byteBuf.readableBytes();
        if (readableBytes == 0) {
            return;
        }
        if (!response.isCommitted()) {
            response.prepareChunkSteam();
        }
        ctx.writeAndFlush(new DefaultHttpContent(byteBuf.copy()));
        byteBuf.clear();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int dataLengthLeftToWrite = len;
        int dataToWriteOffset = off;
        int spaceLeftInCurrentChunk;
        while ((spaceLeftInCurrentChunk = byteBuf.maxWritableBytes()) < dataLengthLeftToWrite) {
            byteBuf.writeBytes(b, dataToWriteOffset, spaceLeftInCurrentChunk);
            dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
            dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
            flush();
        }
        if (dataLengthLeftToWrite > 0) {
            byteBuf.writeBytes(b, dataToWriteOffset, dataLengthLeftToWrite);
        }
    }
}
