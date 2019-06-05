package lu.cecchinel.smarthome.server.handlers;

import com.intrbiz.iot.hs110.HS110Client;
import greycat.Graph;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import model.Constants;
import model.sensors;

import java.nio.ByteBuffer;

public class WMActionHandler {

    public static HttpHandler wmStatusHandler(Graph graph) {
        return exchange -> {
            exchange.getRequestReceiver().receiveFullBytes((httpServerExchange, bytes) -> {
                httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                sensors.find(graph, 0, greycat.Constants.BEGINNING_OF_TIME, "wm", sensors -> {
                    if (sensors.length == 1){
                        HS110Client client = new HS110Client(sensors[0].getIp());
                        try {
                            httpServerExchange.getResponseSender().send(Integer.toString(client.sysInfo().getSystem().getSysInfo().getRelayState()));
                            httpServerExchange.endExchange();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
        };
    }

    public static HttpHandler wmSetRelay(Graph graph, boolean on) {
        return exchange -> {
            exchange.getRequestReceiver().receiveFullBytes((httpServerExchange, bytes) -> {
                httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                sensors.find(graph, 0, greycat.Constants.BEGINNING_OF_TIME, "wm", sensors -> {
                    if (sensors.length == 1){
                        HS110Client client = new HS110Client(sensors[0].getIp());
                        try {
                            client.setRelayState(on);
                            httpServerExchange.endExchange();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
        };
    }

}
