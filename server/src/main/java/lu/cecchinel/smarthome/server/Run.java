package lu.cecchinel.smarthome.server;

import grafana.GrafanaPlugin;
import greycat.Constants;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.leveldb.LevelDBStorage;
import greycat.plugin.Job;
import greycat.plugin.SchedulerAffinity;
import greycat.scheduler.HybridScheduler;
import greycat.websocket.WSSharedServer;
import io.undertow.Handlers;
import model.*;

import static grafana.handlers.GrafanaHandlers.*;
import static greycat.Tasks.newTask;
import static lu.cecchinel.smarthome.server.handlers.ValueHandler.valuesHandler;

public class Run {

    public static void main(String args[]) throws InterruptedException {

        Graph graph = GraphBuilder
                .newBuilder()
                .withPlugin(new ModelPlugin())
                .withPlugin(new GrafanaPlugin())
                .withStorage(new LevelDBStorage(AppProps.getInstance().getProperty(AppProps.DB_PATH)).useNative(false))
                .withScheduler(new HybridScheduler())
                .build();

        WSSharedServer ws_server = new WSSharedServer(graph, Integer.valueOf(AppProps.getInstance().getProperty(AppProps.WS_PORT)));
        ws_server.addHandler("/grafana", Handlers.path().addExactPath("/", rootHandler())
                .addExactPath("/search", searchHandler(graph))
                .addExactPath("/query", queryHandler(graph))
                .addExactPath("/annotations", annotationHandler(graph)))
                .addHandler("/add", Handlers.pathTemplate().add("/value/{name}/{time}/{value}", valuesHandler(graph)));

        ws_server.start();

        graph.connect(isConnected -> {
            sensors.findAll(graph, 0, Constants.BEGINNING_OF_TIME, sensors -> {
                    for (Sensor sensor: sensors){
                        System.out.println(sensor);
                        graph.scheduler().dispatch(SchedulerAffinity.ANY_LOCAL_THREAD, new Job() {
                            @Override
                            public void run() {
                                SmartPlug appliance = new Meross(sensor.getIp());
                                while (true){
                                    double powerValue = ((Meross) appliance).getInstantPower();
                                    sensor.travelInTime(System.currentTimeMillis(), traveledSensor -> {
                                        if (powerValue > 0){
                                            ((Sensor) traveledSensor).setValue(powerValue);
                                        }
                                        graph.save(saveResult -> {
                                            System.out.println( ((Sensor) traveledSensor).getName() + ": " +  ((Sensor) traveledSensor).getValue() + " (av: " + graph.space().available() + ")");
                                            traveledSensor.free();
                                        });
                                    });
                                    try {
                                        Thread.sleep(Long.parseLong(AppProps.getInstance().getProperty(AppProps.DELAY)));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
            });
        });
    }
}
