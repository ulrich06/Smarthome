package lu.cecchinel.smarthome.server;

public class GenericMQTT implements SmartPlug {
    public GenericMQTT(String ip) {
    }

    @Override
    public double getInstantValue() {
        return Double.MIN_VALUE; //Disabled for MQTT sensors as fed by the message handler
    }

    @Override
    public Object getValueFromRawData(Parameter parameter, byte[] rawData) {
        return null;
    }

    @Override
    public byte[] sync() {
        return new byte[0];
    }
}
