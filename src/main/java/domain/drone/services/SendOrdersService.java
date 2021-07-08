package domain.drone.services;
import domain.drone.base.Drone;
import domain.drone.clients.SendOrderClient;
import domain.order.Order;
import domain.other.SyncList;
import utils.Utils;

public class SendOrdersService extends Thread{

    private final MasterService masterService;
    private final SyncList<SendOrderClient> pendingSendOrderClients = new SyncList<>();

    public SendOrdersService(MasterService masterService){ this.masterService = masterService; }

    public MasterService getMasterService(){ return masterService; }

    public void waitToReceiveAllOrdersResponses() throws InterruptedException {
        for(SendOrderClient s: pendingSendOrderClients.getAll()){
            s.join();
        }
    }

    @Override
    public void run(){
        String outputHeader = "SEND ORDERS SERVICE:";
        try{
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            while(true){
                Order orderToSend = masterService.takeOrderAndNotifyIfEmpty();
                Drone targetDrone = masterService.chooseAndBlockDrone(orderToSend);
                if(targetDrone == null){
                    masterService.addOrderIfNotQuitting(orderToSend);
                }else{
                    SendOrderClient sendOrderClient = new SendOrderClient(this, targetDrone, orderToSend);
                    sendOrderClient.start();
                    pendingSendOrderClients.add(sendOrderClient);
                }
            }
        }
        catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}