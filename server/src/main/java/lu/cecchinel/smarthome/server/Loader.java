package lu.cecchinel.smarthome.server;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import grafana.GrafanaPlugin;
import greycat.Callback;
import greycat.Graph;
import greycat.Type;
import model.Constants;
import model.Sensor;
import model.sensors;

import java.io.*;
import java.util.List;

public class Loader {

    public static void convertFromCsvFile(String csvPath, Graph graph, Callback<Boolean> callback) {
        try {
            CSVReader csvReader = new CSVReaderBuilder(new BufferedReader(new FileReader(new File(csvPath))))
                    .withSkipLines(1)
                    .build();


            List<String[]> csvValues = csvReader.readAll();
            csvReader.close();

            for (int i = 0; i < csvValues.size(); i++) {
                String name = csvValues.get(i)[0].trim();
                String ip = csvValues.get(i)[1].trim();
                sensors.find(graph, 0, greycat.Constants.BEGINNING_OF_TIME, name, find -> {
                    if (find.length == 0){
                        Sensor sensor = Sensor.create(0, greycat.Constants.BEGINNING_OF_TIME, graph);
                        sensor.setValue(0.0);
                        sensor.setName(name);
                        sensor.setIp(ip);
                        sensor.set("id", Type.STRING, name);
                        sensors.update(sensor, updated -> {
                            System.out.println("Indexed: " + updated);
                            graph.declareIndex(0, GrafanaPlugin.GRAFANA_INDEX, nodeIndex -> {
                                nodeIndex.update(sensor);
                                graph.save(cb -> {
                                    System.out.println("New sensor: " + sensor);
                                });
                            }, "id");
                        });
                    }
                });
            }
            callback.on(true);

        } catch (FileNotFoundException e) {
            System.err.println("CSV file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("An IO exception occured");
            e.printStackTrace();
        }

    }


}
