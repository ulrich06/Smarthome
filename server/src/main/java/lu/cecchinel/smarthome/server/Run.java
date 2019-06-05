package lu.cecchinel.smarthome.server;

import grafana.GrafanaPlugin;
import greycat.*;
import greycat.Constants;
import greycat.leveldb.LevelDBStorage;
import greycat.mqtt.MQTTPlugin;
import greycat.plugin.Job;
import greycat.plugin.SchedulerAffinity;
import greycat.scheduler.ExecutorScheduler;
import greycat.scheduler.HybridScheduler;
import greycat.scheduler.TrampolineScheduler;
import greycat.websocket.WSSharedServer;
import io.undertow.Handlers;
import lu.cecchinel.smarthome.server.handlers.MQTTHandler;
import model.*;

import static grafana.handlers.GrafanaHandlers.*;
import static greycat.Tasks.newTask;
import static lu.cecchinel.smarthome.server.handlers.ImportExportHandler.exportHandler;
import static lu.cecchinel.smarthome.server.handlers.ValueHandler.valuesHandler;
import static lu.cecchinel.smarthome.server.handlers.WMActionHandler.wmSetRelay;
import static lu.cecchinel.smarthome.server.handlers.WMActionHandler.wmStatusHandler;

public class Run {

    public static void main(String args[]) throws InterruptedException {

        Graph graph = GraphBuilder
                .newBuilder()
                .withPlugin(new ModelPlugin())
                .withPlugin(new GrafanaPlugin())
                .withPlugin(new MQTTPlugin("192.168.178.200", 1883, new String[]{"temperature", "humidity"}, "mqtt", new MQTTHandler("sensors")))
                .withStorage(new LevelDBStorage(AppProps.getInstance().getProperty(AppProps.DB_PATH)).useNative(false))
                .withScheduler(new ExecutorScheduler())
                .build();

        graph.actionRegistry().getOrCreateDeclaration(LoadSensorsAction.NAME).setParams(Type.STRING).setFactory(params -> new LoadSensorsAction((String) params[0]));

        WSSharedServer ws_server = new WSSharedServer(graph, Integer.valueOf(AppProps.getInstance().getProperty(AppProps.WS_PORT)));
        ws_server.addHandler("/grafana", Handlers.path().addExactPath("/", rootHandler())
                                                .addExactPath("/search", searchHandler(graph))
                                                .addExactPath("/query", queryHandler(graph))
                                                .addExactPath("/annotations", annotationHandler(graph)))
                 .addHandler("/add", Handlers.pathTemplate().add("/value/{name}/{time}/{value}", valuesHandler(graph)))
                 .addHandler("/backup", Handlers.path().addExactPath("/export", exportHandler(graph)))
                 .addHandler("/wm", Handlers.path().addExactPath("/", wmStatusHandler(graph))
                                                          .addExactPath("/on", wmSetRelay(graph, true))
                                                          .addExactPath("/off", wmSetRelay(graph, false)));

        ws_server.start();

        graph.connect(isConnected -> {
            DeferCounter counter = graph.newCounter(1);

            counter.then(() -> {
                newTask()
                        .action(LoadSensorsAction.NAME, AppProps.getInstance().getProperty(AppProps.SENSORS_PATH))
                        .travelInTime(Constants.BEGINNING_OF_TIME_STR)
                        .readIndex(sensors.META.name)
                        .forEach(
                                newTask().thenDo(ctx -> {
                                    Sensor sensor = (Sensor) ctx.resultAsNodes().get(0);
                                    String name = sensor.getName();
                                    String ip = sensor.getIp();
                                    String manufacturer = sensor.getManufacturer();

                                    graph.scheduler().dispatch(SchedulerAffinity.ANY_LOCAL_THREAD, () -> {
                                        SmartPlug appliance = null;
                                        switch (manufacturer) {
                                            case "meross":
                                                appliance = new Meross(ip);
                                                break;
                                            case "tplink":
                                                appliance = new TPLink(ip);
                                                break;
                                            case "mqtt":
                                                appliance = new GenericMQTT(ip);
                                                break;
                                            default:
                                                System.err.println("Unknown handler for " + manufacturer);
                                        }

                                        if (appliance != null){
                                            System.out.println("Start loop for " + name);
                                            while (true){
                                                double sensorValue = appliance.getInstantValue();
                                                if (sensorValue > Double.MIN_VALUE){
                                                    newTask()
                                                            .readIndex(sensors.META.name, name)
                                                            .travelInTime(String.valueOf(System.currentTimeMillis()))
                                                            .setAttribute(Sensor.VALUE.name, Sensor.VALUE.type, String.valueOf(sensorValue))
                                                            .save()
                                                            .executeFrom(ctx, ctx.newResult(), SchedulerAffinity.ANY_LOCAL_THREAD, cb -> {});
                                                }
                                                try {
                                                    Thread.sleep(Long.parseLong(AppProps.getInstance().getProperty(AppProps.DELAY)));
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                                    ctx.continueTask();
                                })
                        )
                        .execute(graph, cb -> {
                            if (cb.exception() != null){
                                cb.exception();
                            }
                            System.out.println("Init done.");
                        });

            });

            graph.index(0, 0, GrafanaPlugin.GRAFANA_INDEX, nodeIndex -> {
                if (nodeIndex == null){
                    graph.declareIndex(0, GrafanaPlugin.GRAFANA_INDEX, newIndex -> {
                        newIndex.free();
                        graph.save(saved -> {
                            counter.count();
                        });
                    }, "id");
                } else {
                    nodeIndex.free();
                    counter.count();
                }
            });
        });
    }
}
