/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Echoes back any received data from a client.
 *
 * netty 入门
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        /**
         * 1、Configure the server.  bossGroup用来epoll获取请求  NioEventLoopGroup extends MultithreadEventLoopGroup
         * 2、由于server服务端只绑定1个端口，所以NioEventLoop只启动1个线程即可。
         * 3、创建NioEventLoop，NioEventLoop依赖NioSelector
         *
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        /**
         * 1、worker group用来处理io请求，boss线程处理accept连接事件，worker线程处理具体的io事件，这就是reactor模型，如果handler业务逻辑执行比较耗时，可以交给单独的线程池来处理
         * 2、创建NioEventLoop，线程数量为cpu核数*2
         */
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            /**
             * 创建一个辅助类Bootstrap，就是对我们的Server进行一系列的配置
             *
             */
            ServerBootstrap b = new ServerBootstrap();
            /**
             * ServerBootstrap依赖两个EventLoopGroup：group，chilidGroup，group时只是设置EventLoopGroup
             */
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
              //设置最大等待连接数量
             //.option(ChannelOption.SO_BACKLOG, 100)

             /**
             *  childOption和option的区别？
              */
             .childOption(ChannelOption.SO_BACKLOG, 100)
             //ServerBootstrap 存在handler和childHandler两个属性
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     ByteBuf buf = Unpooled.copiedBuffer("$_".getBytes());
                     //字符串分割符
                     p.addLast(new DelimiterBasedFrameDecoder(1024,buf));
                     //p.addLast(new FixedLengthFrameDecoder(3));
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(serverHandler);
                     p.addLast(new EchoServerHandler2());
                 }
             });

            /**
             * Start the server.
             * 1、在绑定端口的时候，创建并初始化 NioServerSocketChannel
             * 2、绑定端口
             */

            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
