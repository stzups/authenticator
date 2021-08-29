package net.stzups.authenticator;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.stzups.netty.Server;
import net.stzups.netty.TestLog;
import net.stzups.netty.http.DefaultHttpServerHandler;
import net.stzups.netty.http.HttpServerInitializer;
import net.stzups.netty.http.HttpUtils;
import net.stzups.netty.http.exception.HttpException;
import net.stzups.netty.http.exception.exceptions.NotFoundException;
import net.stzups.netty.http.handler.HttpHandler;

public class Authenticator {
    public static void main(String[] args) throws Exception {
        try (Server server = new Server(8080)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));

            ChannelFuture closeFuture = server.start(new HttpServerInitializer(new HttpServerInitializer.Config() {
                @Override
                public boolean getSSL() {
                    return false;
                }

                @Override
                public String getSSLRootPath() {
                    return null;
                }

                @Override
                public String getSSLPath() {
                    return null;
                }

                @Override
                public boolean getDebugLogTraffic() {
                    return false;
                }
            }) {
                @Override
                protected void initChannel(SocketChannel channel) {
                    TestLog.setLogger(channel);
                    super.initChannel(channel);

                    channel.pipeline()
                            .addLast(new HttpContentCompressor())
                            .addLast(new ChunkedWriteHandler())
                            .addLast(new DefaultHttpServerHandler()
                                    .addLast(new HttpHandler("/") {
                                        @Override
                                        public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response) throws HttpException {
                                            System.out.println(request.headers());
                                            HttpUtils.send(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED));
                                            return true;
                                        }
                                    }));
                }
            });

            System.err.println("Started server");
            closeFuture.sync();
            System.err.println("Server closed");
        }
    }
}
