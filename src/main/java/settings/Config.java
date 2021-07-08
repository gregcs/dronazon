package settings;

public class Config {

    public static final int PRINT_LEVEL = 1;

    public static final int TIMEOUT_ORDER_GENERATION = 5000;
    public static final int SLEEP_TIME_DELIVERY = 5000;
    public static final int TIMEOUT_PRINT_INFORMATION = 10000;
    public static final int TIMEOUT_SEND_GLOBAL_STATS = 10000;
    public static final int TIMEOUT_SET_MASTER = 5000;
    public static final int DEFAULT_GRPC_TIMEOUT = 20;

    public static final int SMART_CITY_DIMENSION = 10;
    public static final String BROKER_ADDRESS = "tcp://localhost:1883";
    public static final String NEW_ORDER_TOPIC = "dronazon_pack/smartcity/orders/";

    public static final String ADMINISTRATOR_SERVER_HOSTNAME = "localhost";
    public static final String ADMINISTRATOR_SERVER_PORT = "1337";
    public static final String ADMINISTRATOR_SERVER_ADDRESS = String.format("http://%s:%s/",
            Config.ADMINISTRATOR_SERVER_HOSTNAME, Config.ADMINISTRATOR_SERVER_PORT);

    public static final String DRONE_HOSTNAME = "localhost";
    public static final int DRONE_ID_UPPER_BOUND = 1000;

    public static final int DEFAULT_BATTERY_LEVEL = 100;
    public static final int PERCENTAGE_BATTERY_DECAY = 10;
    public static final double PERCENTAGE_BATTERY_LIMIT_TO_QUIT = 15;

    public static final int WINDOW_SIZE = 8;
    public static final int WINDOW_OVERLAP = 50;

    public static final double DOUBLE_TOLERANCE = 0.1;

    public static final int DEFAULT_QOS = 2;
}
