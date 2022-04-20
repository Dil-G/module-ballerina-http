package io.ballerina.stdlib.http.transport.contractimpl.listener.states.http3;

import io.ballerina.stdlib.http.transport.contract.HttpResponseFuture;
import io.ballerina.stdlib.http.transport.contract.ServerConnectorFuture;
import io.ballerina.stdlib.http.transport.contractimpl.Http3OutboundRespListener;
import io.ballerina.stdlib.http.transport.contractimpl.common.Util;
import io.ballerina.stdlib.http.transport.contractimpl.common.states.Http3MessageStateContext;
import io.ballerina.stdlib.http.transport.contractimpl.common.states.StateUtil;
import io.ballerina.stdlib.http.transport.contractimpl.listener.Http3ServerChannelInitializer;
import io.ballerina.stdlib.http.transport.contractimpl.listener.http3.Http3SourceHandler;
import io.ballerina.stdlib.http.transport.message.HttpCarbonMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.ballerina.stdlib.http.transport.contract.Constants.*;
import static io.ballerina.stdlib.http.transport.contract.Constants.REMOTE_CLIENT_CLOSED_WHILE_WRITING_OUTBOUND_RESPONSE_BODY;

public class SendingEntityBody implements ListenerState{

    private static final Logger LOG = LoggerFactory.getLogger(SendingEntityBody.class);
    private static final InternalLogger ACCESS_LOGGER = InternalLoggerFactory.getInstance(ACCESS_LOG);

    private final Http3OutboundRespListener http3OutboundRespListener;
    private final Http3MessageStateContext http3MessageStateContext;
    private final ChannelHandlerContext ctx;
    private final Http3ServerChannelInitializer serverChannelInitializer;
    private final HttpCarbonMessage inboundRequestMsg;
    private final HttpResponseFuture outboundRespStatusFuture;
    private final Object inboundRequestArrivalTime;
    private final long streamId;
    private String remoteAddress;
    private HttpCarbonMessage outboundResponseMsg;
    private Long contentLength = 0L;

    public SendingEntityBody(Http3OutboundRespListener http3OutboundRespListener, Http3MessageStateContext
            http3MessageStateContext, long streamId) {
        this.http3OutboundRespListener = http3OutboundRespListener;
        this.http3MessageStateContext = http3MessageStateContext;
        this.ctx = http3OutboundRespListener.getChannelHandlerContext();
        this.serverChannelInitializer = http3OutboundRespListener.getServerChannelInitializer();
        this.inboundRequestMsg = http3OutboundRespListener.getInboundRequestMsg();
        this.outboundRespStatusFuture = inboundRequestMsg.getHttpOutboundRespStatusFuture();
        this.inboundRequestArrivalTime = http3OutboundRespListener.getInboundRequestArrivalTime();
        this.streamId = streamId;
        this.remoteAddress = http3OutboundRespListener.getRemoteAddress();

    }

    @Override
    public void readInboundRequestHeaders(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame, long streamId) {
        LOG.warn("readInboundRequestHeaders is not a dependant action of this state");

    }

    @Override
    public void readInboundRequestBody(Http3SourceHandler http3SourceHandler, Http3DataFrame dataFrame, boolean isLast)
            throws Http3Exception {
        // In bidirectional streaming case, while sending the request data frames, server response data frames can
        // receive. In order to handle it. we need to change the states depending on the action.
        http3MessageStateContext.setListenerState(new ReceivingEntityBody(http3MessageStateContext, streamId));
        http3MessageStateContext.getListenerState().readInboundRequestBody(http3SourceHandler, dataFrame, isLast);
    }

    @Override
    public void writeOutboundResponseHeaders(Http3OutboundRespListener http3OutboundRespListener, HttpCarbonMessage
            outboundResponseMsg, HttpContent httpContent, long streamId) throws Http3Exception {
        LOG.warn("writeOutboundResponseHeaders is not a dependant action of this state");

    }

    @Override
    public void writeOutboundResponseBody(Http3OutboundRespListener http3OutboundRespListener, HttpCarbonMessage
            outboundResponseMsg, HttpContent httpContent, long streamId) throws Http3Exception {
        this.outboundResponseMsg = outboundResponseMsg;
        writeContent(http3OutboundRespListener, outboundResponseMsg, httpContent, streamId);
    }

    @Override
    public void handleStreamTimeout(ServerConnectorFuture serverConnectorFuture, ChannelHandlerContext ctx,
                                    Http3OutboundRespListener http3OutboundRespListener, long streamId) {

    }

    @Override
    public void handleAbruptChannelClosure(ServerConnectorFuture serverConnectorFuture) {
        IOException connectionClose = new IOException(REMOTE_CLIENT_CLOSED_WHILE_WRITING_OUTBOUND_RESPONSE_BODY);
        outboundResponseMsg.setIoException(connectionClose);
        outboundRespStatusFuture.notifyHttpListener(connectionClose);

        LOG.error(REMOTE_CLIENT_CLOSED_WHILE_WRITING_OUTBOUND_RESPONSE_BODY);

    }

    private void writeContent(Http3OutboundRespListener http3OutboundRespListener,
                              HttpCarbonMessage outboundResponseMsg, HttpContent httpContent, long streamId) {
        if (httpContent instanceof LastHttpContent) {

            final LastHttpContent lastContent = (httpContent == LastHttpContent.EMPTY_LAST_CONTENT) ?
                    new DefaultLastHttpContent() : (LastHttpContent) httpContent;
            HttpHeaders trailers = lastContent.trailingHeaders();
            trailers.add(outboundResponseMsg.getTrailerHeaders());
            boolean endStream = trailers.isEmpty();
            writeData(lastContent, streamId, endStream);
            if (!trailers.isEmpty()) {
                Http3HeadersFrame http2Trailers = toHttp3Headers(trailers, true);
                // Write trailing headers.

                ChannelFuture channelFuture = ctx.write(http2Trailers, ctx.newPromise());

                StateUtil.notifyIfHeaderWriteFailure(outboundRespStatusFuture, channelFuture,
                        REMOTE_CLIENT_CLOSED_BEFORE_INITIATING_OUTBOUND_RESPONSE);
            }
            http3MessageStateContext
                    .setListenerState(new ResponseCompleted(http3OutboundRespListener,
                            http3MessageStateContext, streamId));
        } else {
            writeData(httpContent, streamId, false);
        }
    }

    private void writeData(HttpContent httpContent, long streamId, boolean endStream) {
            contentLength += httpContent.content().readableBytes();
            final ByteBuf content = httpContent.content();

        ChannelFuture channelFuture = ctx.writeAndFlush(new DefaultHttp3DataFrame(
                Unpooled.wrappedBuffer(content)), ctx.newPromise()).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);

        StateUtil.notifyIfHeaderWriteFailure(outboundRespStatusFuture, channelFuture,
                REMOTE_CLIENT_CLOSED_BEFORE_INITIATING_OUTBOUND_RESPONSE);
            if (endStream) {
                http3OutboundRespListener.getHttp3ServerChannel().getStreamIdRequestMap().remove(streamId);
                Util.checkForResponseWriteStatus(inboundRequestMsg, outboundRespStatusFuture, channelFuture);
            } else {
                StateUtil.notifyIfHeaderWriteFailure(outboundRespStatusFuture, channelFuture,
                        REMOTE_CLIENT_CLOSED_BEFORE_INITIATING_OUTBOUND_RESPONSE);
            }
        }

    //FROM NETTY
    static Http3HeadersFrame toHttp3Headers(HttpHeaders inHeaders, boolean validateHeaders) {
        if (inHeaders.isEmpty()) {
            return new DefaultHttp3HeadersFrame();
        }
        final Http3Headers headers = new DefaultHttp3Headers(validateHeaders, inHeaders.size());
        final Http3HeadersFrame out = new DefaultHttp3HeadersFrame(headers);
        return out;
    }


}