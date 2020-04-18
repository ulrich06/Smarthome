package lu.cecchinel.smarthome.server.handlers;

import greycat.mqtt.MessageHandler;
import model.sensors;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTHandler extends MessageHandler {
    /**
     * Build the message handler
     *
     * @param lookupIndex Index's name containing the targeted nodes
     */
    public MQTTHandler(String lookupIndex) {
        super(lookupIndex);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        if (s.split("/").length == 3){
            String message = new String(mqttMessage.getPayload());
            System.out.println(message);

        } else {
            // assume its smartcampus format
            String message = new String(mqttMessage.getPayload());
            String[] rawData = message.split(";");
            String sensor = rawData[0];
            double value = Double.parseDouble(rawData[1]);
            long timestamp = System.currentTimeMillis();

            if (rawData.length == 3) {
                timestamp = Long.parseLong(rawData[2]) * 1000;
            }

            sensors.find(super.getGraph(), 0, timestamp, sensor, result -> {
                if (result.length == 1){
                    result[0].setValue(value);
                    super.getGraph().freeNodes(result);
                }
            });
        }



    }
}
