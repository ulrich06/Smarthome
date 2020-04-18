package lu.cecchinel.smarthome.server;

import grafana.GrafanaPlugin;
import greycat.*;
import greycat.internal.task.TaskHelper;
import greycat.struct.Buffer;
import model.Sensor;
import model.sensors;

import static greycat.Tasks.newTask;

public class CreateSensorAction implements Action {
    public final static String NAME = "CreateSensor";
    private final String name;
    private final String manufacturer;
    private final String ip;

    private Task createTask = newTask()
            .log("New sensor: {{name}}")
            .createTypedNode(Sensor.META.name)
            .setAttribute(Sensor.NAME.name, Sensor.NAME.type, "{{name}}")
            .setAttribute(Sensor.MANUFACTURER.name, Sensor.MANUFACTURER.type, "{{manufacturer}}")
            .setAttribute(Sensor.IP.name, Sensor.IP.type, "{{ip}}")
            .setAttribute("id", Type.STRING, "{{name}}")
            .setAttribute(Sensor.VALUE.name, Sensor.VALUE.type, "0.0")
            .setAsVar("newSensor")
            .updateIndex(sensors.META.name)
            .readVar("newSensor")
            .updateIndex(GrafanaPlugin.GRAFANA_INDEX);

    public CreateSensorAction(String name, String manufacturer, String ip){
        this.name = name;
        this.manufacturer = manufacturer;
        this.ip = ip;
    }

    @Override
    public void eval(TaskContext ctx) {
        TaskContext preparedTc = createTask.prepare(ctx.graph(), null, result -> {
            ctx.result().free();
            if (result.exception() != null){
                result.exception().printStackTrace();
                ctx.endTask(null, result.exception());
            } else {
                ctx.continueTask();
            }
        });

        preparedTc.setTime(Constants.BEGINNING_OF_TIME);
        preparedTc.setVariable("name", ctx.template(this.name));
        preparedTc.setVariable("manufacturer", ctx.template(this.manufacturer));
        preparedTc.setVariable("ip", ctx.template(this.ip));

        createTask.executeUsing(preparedTc);
    }

    @Override
    public void serialize(Buffer builder) {
        builder.writeString(name());
        builder.writeChar(greycat.Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(name + "", builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(manufacturer + "", builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(ip + "", builder, true);
        builder.writeChar(greycat.Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public String name() {
        return NAME;
    }
}
