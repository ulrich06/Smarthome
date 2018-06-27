package lu.cecchinel.smarthome.server;

import grafana.GrafanaPlugin;
import greycat.Constants;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Type;
import greycat.leveldb.LevelDBStorage;
import greycat.scheduler.HybridScheduler;
import model.*;

public class BuildDataStructure {

    public static void main(String[] args){
        Graph graph = GraphBuilder
                .newBuilder()
                .withPlugin(new ModelPlugin())
                .withPlugin(new GrafanaPlugin())
                .withStorage(new LevelDBStorage(AppProps.getInstance().getProperty(AppProps.DB_PATH)).useNative(false))
                .build();

        graph.connect(isConnected -> {
            Loader.convertFromCsvFile(AppProps.getInstance().getProperty(AppProps.SENSORS_PATH), graph, cb -> {
                graph.save(saved -> {
                    System.out.println("done");
                });
            });

        });
    }
}
