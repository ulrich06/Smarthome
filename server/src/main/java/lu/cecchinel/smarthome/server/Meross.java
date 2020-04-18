package lu.cecchinel.smarthome.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Meross implements SmartPlug{

    private final String ip;

    public Meross(String ip){
        this.ip = ip;
    }

    public Object getValueFromRawData(Parameter parameter, byte[] rawData) throws RuntimeException {
        if (rawData == null){
            throw new RuntimeException("No raw data to parse");
        }
        switch (parameter){
            case INSTANT_POWER: default:
                Pattern p = Pattern.compile("P:((\\d){1,6})");
                Matcher matcher = p.matcher(new String(rawData));
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1)) / 1000; //Convert in Watts
                }
                throw new RuntimeException("No power value found in: \n" + rawData);
        }
    }

    public byte[] sync() {
        long begin = System.currentTimeMillis();
        try {
            Socket pingSocket;
            PrintWriter out;
            BufferedReader in;
            try {
                pingSocket = new Socket(ip, 23);
                out = new PrintWriter(pingSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            out.println("admin\r\n\r\nmeross\r\npower dump\r\nexit\r\n");
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            long now;
            do {
                now = System.currentTimeMillis();
                line = in.readLine();
                stringBuilder.append(line).append("\n");
            } while (!line.contains("current year :") && (now - begin) < 5 * 1000 * 1000);;
            //System.out.println(stringBuilder.toString());
            out.close();
            in.close();
            pingSocket.close();
            return stringBuilder.toString().getBytes();
        } catch (IOException e) {
            System.err.println("Error while sync()");
            e.printStackTrace();
        }
        return null;
    }

    public double getInstantValue() {
        try {
            return (double) getValueFromRawData(Parameter.INSTANT_POWER, sync());
        } catch (RuntimeException ignored){ }
        return -Double.MAX_VALUE;

    }
}
