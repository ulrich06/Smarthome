package lu.cecchinel.smarthome.server;


import grafana.GrafanaPlugin;
import greycat.Action;
import greycat.Constants;
import greycat.TaskContext;
import greycat.Type;
import greycat.internal.task.TaskHelper;
import greycat.plugin.SchedulerAffinity;
import greycat.struct.Buffer;
import model.Sensor;
import model.sensors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static greycat.Tasks.newTask;

public class LoadSensorsAction implements Action {
    public final static String NAME = "LoadSensors";
    private final String path;

    public LoadSensorsAction(String pathToCSV){
        this.path = pathToCSV;
    }
    @Override
    public void eval(TaskContext taskContext) {
        newTask().thenDo(ctx -> {
            Path dataFile = Paths.get(ctx.resultAsStrings().get(0));
            try {
                ArrayList<String> fileContent = new ArrayList<>();

                BufferedReader br = new BufferedReader(new FileReader(dataFile.toFile()));
                String line;
                while ((line = br.readLine()) != null){
                    fileContent.add(line);
                }

                String[] lines = fileContent.toArray(new String[0]);
                ctx.continueWith(ctx.wrap(lines));

            } catch (Exception e){
                ctx.endTask(ctx.result(), e);
            }
        }).forEach(
                newTask()
                        .declareVar("name")
                        .declareVar("ip")
                        .declareVar("manufacturer")
                        .thenDo(ctx -> {
                            String line = ctx.resultAsStrings().get(0);
                            String[] bareDataline = line.split(",");
                            ctx.setVariable("name", bareDataline[0]);
                            ctx.setVariable("ip", bareDataline[1]);
                            ctx.setVariable("manufacturer", bareDataline[2]);
                            ctx.continueTask();
                        })
                        .travelInTime(Constants.BEGINNING_OF_TIME_STR)
                        .readIndex(sensors.META.name, "{{name}}")
                        .ifThenElse(cond -> cond.result().size() == 0,
                                newTask()
                                        .action(CreateSensorAction.NAME, "{{name}}", "{{manufacturer}}", "{{ip}}"),
                                newTask()
                                        .travelInTime(Constants.BEGINNING_OF_TIME_STR)
                                        .setAttribute(Sensor.MANUFACTURER.name, Sensor.MANUFACTURER.type, "{{manufacturer}}")
                                        .setAttribute(Sensor.IP.name, Sensor.IP.type, "{{ip}}")
                                        .setAttribute(Sensor.VALUE.name, Sensor.VALUE.type, "0.0")
                                        .updateIndex(GrafanaPlugin.GRAFANA_INDEX)
                        ).save()

        ).executeFrom(taskContext, taskContext.newResult().add(this.path), SchedulerAffinity.ANY_LOCAL_THREAD, cb -> {
            if (cb.exception() != null){
                cb.exception();
            }
            taskContext.continueWith(cb);
        });
    }

    @Override
    public void serialize(Buffer builder) {
        builder.writeString(name());
        builder.writeChar(greycat.Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(path + "", builder, true);
        builder.writeChar(greycat.Constants.TASK_PARAM_CLOSE);

    }

    @Override
    public String name() {
        return NAME;
    }
}
