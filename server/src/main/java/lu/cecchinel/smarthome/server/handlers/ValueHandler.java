package lu.cecchinel.smarthome.server.handlers;

import greycat.Graph;
import greycat.Type;
import io.undertow.server.HttpHandler;
import model.Sensor;
import model.sensors;

import static greycat.Tasks.newTask;

public class ValueHandler {

    public static HttpHandler valuesHandler(Graph graph) {
        return httpServerExchange -> {
            try {
                String name = httpServerExchange.getQueryParameters().get("name").getFirst();
                Long time = Long.valueOf(httpServerExchange.getQueryParameters().get("time").getFirst());
                Double value = Double.valueOf(httpServerExchange.getQueryParameters().get("value").getFirst());
                newTask().readIndex(sensors.META.name, name)
                        .travelInTime(String.valueOf(time))
                        .setAttribute(Sensor.VALUE.name, Type.DOUBLE, String.valueOf(value))
                        .save()
                        .execute(graph, cb -> {
                            if (cb.exception() != null) cb.exception().printStackTrace();
                        });

            } catch (NullPointerException e) {
                System.out.println("Incomplete request: " + httpServerExchange.getQueryString());
            }

        };
    }
}
