package lu.cecchinel.smarthome.server.handlers;


import greycat.Graph;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ImportExportHandler {

    public static HttpHandler exportHandler(Graph graph) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sensors", new JSONArray());
        return httpServerExchange -> {
            httpServerExchange.getResponseSender().send(jsonObject.toJSONString());
        };
    }
}
