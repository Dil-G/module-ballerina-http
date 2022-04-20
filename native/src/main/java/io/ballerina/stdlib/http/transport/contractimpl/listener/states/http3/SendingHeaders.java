package io.ballerina.stdlib.http.transport.contractimpl.listener.states.http3;

import io.ballerina.stdlib.http.transport.contract.HttpResponseFuture;
import io.ballerina.stdlib.http.transport.contract.ServerConnectorFuture;
import io.ballerina.stdlib.http.transport.contract.exceptions.ServerConnectorException;
import io.ballerina.stdlib.http.transport.contractimpl.Http3OutboundRespListener;
import io.ballerina.stdlib.http.transport.contractimpl.common.Util;
import io.ballerina.stdlib.http.transport.contractimpl.common.states.Http3MessageStateContext;
import io.ballerina.stdlib.http.transport.contractimpl.common.states.Http3StateUtil;
import io.ballerina.stdlib.http.transport.contractimpl.common.states.StateUtil;
import io.ballerina.stdlib.http.transport.contractimpl.listener.http3.Http3SourceHandler;
import io.ballerina.stdlib.http.transport.message.HttpCarbonMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.incubator.codec.http3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.ballerina.stdlib.http.transport.contract.Constants.*;

public class SendingHeaders implements ListenerState {

    private static final Logger LOG = LoggerFactory.getLogger(SendingHeaders.class);

    private static Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();

    private final Http3OutboundRespListener http3OutboundRespListener;
    private final Http3MessageStateContext http3MessageStateContext;
    private final ChannelHandlerContext ctx;
    private final HttpResponseFuture outboundRespStatusFuture;
    private final HttpCarbonMessage inboundRequestMsg;
    private final long streamId;

    public SendingHeaders(Http3OutboundRespListener http3OutboundRespListener,
                          Http3MessageStateContext http3MessageStateContext) {
        this.http3OutboundRespListener =  http3OutboundRespListener;
        this.http3MessageStateContext = http3MessageStateContext;
        this.ctx =  http3OutboundRespListener.getChannelHandlerContext();
        this.inboundRequestMsg =  http3OutboundRespListener.getInboundRequestMsg();
        this.outboundRespStatusFuture =  inboundRequestMsg.getHttpOutboundRespStatusFuture();
        this.streamId =  http3OutboundRespListener.getStreamId();
    }

    @Override
    public void readInboundRequestHeaders(ChannelHandlerContext ctx,
                                          Http3HeadersFrame headersFrame, long streamId) throws Http3Exception {
        LOG.warn("readInboundRequestHeaders is not a dependant action of this state");
    }

    @Override
    public void readInboundRequestBody(Http3SourceHandler http3SourceHandler,
                                       Http3DataFrame dataFrame, boolean isLast) throws Http3Exception {
        http3MessageStateContext.setListenerState(new ReceivingEntityBody(http3MessageStateContext, streamId));
        http3MessageStateContext.getListenerState().readInboundRequestBody(http3SourceHandler, dataFrame, isLast);

    }

    @Override
    public void writeOutboundResponseHeaders(Http3OutboundRespListener http3OutboundRespListener,
                                             HttpCarbonMessage outboundResponseMsg, HttpContent httpContent,
                                             long streamId) throws Http3Exception {
        writeHeaders(outboundResponseMsg, streamId);
        http3MessageStateContext.setListenerState(
                new SendingEntityBody(http3OutboundRespListener, http3MessageStateContext, streamId));
        http3MessageStateContext.getListenerState()
                .writeOutboundResponseBody(http3OutboundRespListener, outboundResponseMsg, httpContent, streamId);
    }


    @Override
    public void writeOutboundResponseBody(Http3OutboundRespListener http3OutboundRespListener,
                                          HttpCarbonMessage outboundResponseMsg, HttpContent httpContent,
                                          long streamId) throws Http3Exception {
        writeOutboundResponseHeaders(http3OutboundRespListener, outboundResponseMsg, httpContent, streamId);
    }

    @Override
    public void handleStreamTimeout(ServerConnectorFuture serverConnectorFuture,
                                    ChannelHandlerContext ctx, Http3OutboundRespListener http3OutboundRespListener,
                                    long streamId) {
        try {
            serverConnectorFuture.notifyErrorListener(
                    new ServerConnectorException(IDLE_TIMEOUT_TRIGGERED_WHILE_WRITING_OUTBOUND_RESPONSE_HEADERS));
            LOG.error(IDLE_TIMEOUT_TRIGGERED_WHILE_WRITING_OUTBOUND_RESPONSE_HEADERS);
        } catch (ServerConnectorException e) {
            LOG.error("Error while notifying error state to server-connector listener");
        }
    }

    @Override
    public void handleAbruptChannelClosure(ServerConnectorFuture serverConnectorFuture) {
        IOException connectionClose = new IOException(REMOTE_CLIENT_CLOSED_WHILE_WRITING_OUTBOUND_RESPONSE_BODY);
        http3OutboundRespListener.getOutboundResponseMsg().setIoException(connectionClose);
        outboundRespStatusFuture.notifyHttpListener(connectionClose);

        LOG.error(REMOTE_CLIENT_CLOSED_WHILE_WRITING_OUTBOUND_RESPONSE_HEADERS);
    }

    private void writeHeaders(HttpCarbonMessage outboundResponseMsg, long streamId) {
        // Construct Http3 headers
        outboundResponseMsg.getHeaders().
                add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HTTP_SCHEME);
        StateUtil.addTrailerHeaderIfPresent(outboundResponseMsg);
        String serverName = null;
        HttpMessage httpMessage =
                Util.createHttpResponse(outboundResponseMsg, HTTP3_VERSION, serverName, true);

        headersFrame = Http3StateUtil.toHttp3Headers(httpMessage, true);

        ChannelFuture channelFuture = ctx.write(headersFrame, ctx.newPromise());

        StateUtil.notifyIfHeaderWriteFailure(outboundRespStatusFuture, channelFuture,
                REMOTE_CLIENT_CLOSED_BEFORE_INITIATING_OUTBOUND_RESPONSE);

        http3MessageStateContext.setHeadersSent(true);
    }


}