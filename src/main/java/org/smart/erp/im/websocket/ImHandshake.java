package org.smart.erp.im.websocket;

import org.smart.erp.im.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Component
public class ImHandshake implements HandshakeInterceptor {

    private final ImTickets tickets;

    private final ImProperties properties;

    public ImHandshake(ImTickets tickets, ImProperties properties) {
        this.tickets = tickets;
        this.properties = properties;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
            Map<String, Object> attributes) {
        String origin = request.getHeaders().getOrigin();
        if (origin != null && !properties.getAllowedOrigins().contains(origin)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        try {
            var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            if (query.size() != 1 || query.get("ticket") == null || query.get("ticket").size() != 1)
                throw ImException.invalid("连接仅接受一个ticket参数");
            attributes.put("imTicket", tickets.consume(query.getFirst("ticket")));
            return true;
        }
        catch (ImException e) {
            response.setStatusCode(HttpStatus.valueOf(e.status()));
            return false;
        }
        catch (RuntimeException e) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
            Exception exception) {
    }

}
