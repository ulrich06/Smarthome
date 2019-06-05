package lu.cecchinel.smarthome.server;

import greycat.plugin.Job;

public interface SmartPlug{

    public double getInstantValue();
    Object getValueFromRawData(Parameter parameter, byte[] rawData);
    byte[] sync();



}
