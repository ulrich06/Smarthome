package lu.cecchinel.smarthome.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class AppProps {

    public static final String DB_PATH = "db_path";
    public static final String WS_PORT = "ws_port";
    public static final String SENSORS_PATH = "sensors_path";
    public static final String DELAY = "delay";

    private static AppProps instance = null;
    private Properties userProps = new Properties();
    private Properties defaultProperties = new Properties();

    private AppProps() throws Exception {
        FileInputStream reader;
        File userFile = new File("user.properties");
        defaultProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
        if (userFile.exists()) {
            reader = new FileInputStream(userFile);
            this.userProps.load(reader);
        }
    }

    public static AppProps getInstance() {
        if (instance == null) {
            try {
                instance = new AppProps();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }


    public String getProperty(String name) {
        return userProps.getProperty(name, defaultProperties.getProperty(name));
    }

}

