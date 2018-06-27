package lu.cecchinel.smarthome.server;

import greycat.plugin.Job;

public interface SmartPlug{

    Object getValueFromRawData(Parameter parameter, byte[] rawData);
    byte[] sync();

}
