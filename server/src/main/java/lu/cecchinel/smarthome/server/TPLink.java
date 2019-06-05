package lu.cecchinel.smarthome.server;

import com.intrbiz.iot.hs110.HS110Client;

public class TPLink implements SmartPlug {

    private final String ip;
    HS110Client client;

    public TPLink(String ip) {
        this.ip = ip;
        this.client = new HS110Client(ip);
    }


    @Override
    public double getInstantValue() {

        try {
            return client.consumption().getPower() / 1000;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Double.MIN_VALUE;
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
