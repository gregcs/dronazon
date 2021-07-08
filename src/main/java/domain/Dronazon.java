package domain;

import domain.order.Order;
import domain.other.Mapper;
import domain.exceptions.InvalidOrder;
import domain.exceptions.InvalidPosition;
import org.eclipse.paho.client.mqttv3.*;
import settings.Config;
import utils.Utils;

public class Dronazon{

    private final MqttClient mqttClient;
    private final String outputHeader = "DRONAZON:";

    public Dronazon(MqttClient mqttClient){
        this.mqttClient = mqttClient;
    }

    private void connect() throws MqttException {
        if(mqttClient.isConnected()) {
            Utils.printIfLevel(String.format("%s Client is already connected to the Broker", outputHeader),0);
        }else{
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Utils.printIfLevel(String.format("%s Client is trying to connect to the Broker ...", outputHeader), 2);
            mqttClient.connect(connOpts);
            Utils.printIfLevel(String.format("%s Client has connected to the Broker", outputHeader), 0);
        }
    }

    private void setCallback(){
        mqttClient.setCallback(new MqttCallback() {
            public void messageArrived(String topic, MqttMessage message) {
                // Not used Here
            }
            public void connectionLost(Throwable t) {
                Utils.printIfLevel(String.format("%s Connection lost | %s", outputHeader, t.getMessage()), 0);
                Utils.traceIfTest(t);
            }
            public void deliveryComplete(IMqttDeliveryToken token) {
                if (token.isComplete()) {
                    Utils.printIfLevel(String.format("%s Order delivered", outputHeader), 0);
                }
            }
        });
    }

    private void publishOrder() throws MqttException {
        try {
            Order o = new Order();
            MqttMessage message = new MqttMessage(Mapper.serializeOrderToByteArray(o));
            message.setQos(Config.DEFAULT_QOS);
            mqttClient.publish(Config.NEW_ORDER_TOPIC, message);
            Utils.printIfLevel(String.format("%s %s published", outputHeader, o), 0);
        } catch (InvalidOrder | InvalidPosition e) {
            Utils.printIfLevel(String.format("%s Invalid order", outputHeader),1);
        }
    }

    private synchronized void waitTime() throws InterruptedException { wait(Config.TIMEOUT_ORDER_GENERATION); }

    public void repeatedPublishOrder() throws MqttException, InterruptedException {
        while(true){
            publishOrder();
            waitTime();
        }
    }

    public static Dronazon createMqttConnectedPublisher(String brokerAddress) throws MqttException {
        Dronazon dronazon = new Dronazon(new MqttClient(brokerAddress, MqttClient.generateClientId()));
        dronazon.connect();
        dronazon.setCallback();
        return dronazon;
    }

    /*
    public void disconnect(){
        if (!mqttClient.isConnected()){
            Utils.printIfLevel(String.format("%s Client is already disconnected", outputHeader), 0);
        }else{
            try {
                mqttClient.disconnect();
                Utils.printIfLevel(String.format("%s Client disconnected", outputHeader), 0);
            } catch (MqttException e) {
                Utils.printIfLevel(String.format("%s Disconnection error", outputHeader),0);
            }
        }
    }
    */

    public static void main(String[] args) {
        try {
            Dronazon dronazon = Dronazon.createMqttConnectedPublisher(Config.BROKER_ADDRESS);
            dronazon.repeatedPublishOrder();
        } catch (MqttException | InterruptedException e) {
            System.out.println("DRONAZON ERROR");
            e.printStackTrace();
        }finally {
            System.out.println("DRONAZON TERMINATED");
        }
    }

}