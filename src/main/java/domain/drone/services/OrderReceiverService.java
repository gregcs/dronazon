package domain.drone.services;

import grpc.domain.OrderOuterClass;
import domain.order.Order;
import domain.other.Mapper;

import org.eclipse.paho.client.mqttv3.*;
import settings.Config;
import utils.Utils;

public class OrderReceiverService extends Thread{

    private final MasterService masterService;
    private final String broker;
    private final String clientId;
    private MqttClient mqttClient;
    private final String topic;
    private final int qos;
    private final String outputHeader = "ORDER RECEIVER SERVICE:";

    public OrderReceiverService(MasterService masterService){
        this.masterService = masterService;
        this.broker = Config.BROKER_ADDRESS;
        this.clientId = MqttClient.generateClientId();
        this.topic = Config.NEW_ORDER_TOPIC;
        this.qos = Config.DEFAULT_QOS;
    }

    private void connect() throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        Utils.printIfLevel(String.format("%s MQTT CLIENT IS CONNECTING TO THE BROKER %s", outputHeader, broker),2);
        mqttClient.connect(connOpts);
        Utils.printIfLevel(String.format("%s MQTT CLIENT| CONNECTED TO THE BROKER %s", outputHeader, broker), 2);
    }

    private void setCallback(){
        mqttClient.setCallback(new MqttCallback() {
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    Order order = Mapper.deserializeOrderMessage(OrderOuterClass.Order.parseFrom(message.getPayload()));
                    Utils.printIfLevel(String.format("%s MQTT CLIENT| order arrived -> %s", outputHeader, order),2);
                    masterService.addOrder(order);
                } catch (Throwable t) {
                    Utils.printIfLevel(String.format("%s MQTT CLIENT| MESSAGE ARRIVED ERROR", outputHeader),1);
                    Utils.traceIfTest(t);
                }
            }
            public void connectionLost(Throwable cause) {
                Utils.printIfLevel(String.format("%s MQTT CLIENT| CONNECTION LOST -> CAUSE: %s", outputHeader, cause.getMessage()),1);
            }
            public void deliveryComplete(IMqttDeliveryToken token) { }
        });
    }

    private void subscribe() throws MqttException {
        Utils.printIfLevel(String.format("%s MQTT CLIENT SUBSCRIBING TO THE TOPIC %s", outputHeader, topic),2);
        mqttClient.subscribe(topic,qos);
        Utils.printIfLevel(String.format("%s MQTT CLIENT SUBSCRIBED TO THE TOPIC %s", outputHeader, topic),2);
    }

    public void initialize() throws MqttException {
        mqttClient =  new MqttClient(broker, clientId);
        connect();
        setCallback();
        subscribe();
    }

    public void disconnect() {
        try {
            Utils.printIfLevel(String.format("%s MQTT CLIENT| DISCONNECTING FROM THE BROKER", outputHeader),2);
            mqttClient.disconnect();
            Utils.printIfLevel(String.format("%s MQTT CLIENT| DISCONNECTED FROM THE BROKER", outputHeader),1);
            Utils.printIfLevel(String.format("%s ENDED", outputHeader),2);
        } catch (MqttException e) {
            Utils.printIfLevel(String.format("%s MQTT CLIENT| DISCONNECTION ERROR", outputHeader),1);
        }
    }

    @Override
    public void run(){
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            initialize();
        }catch(Throwable t) {
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }
    }

}
