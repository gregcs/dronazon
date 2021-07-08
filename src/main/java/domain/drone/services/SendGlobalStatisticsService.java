package domain.drone.services;

import domain.other.WebServiceClient;
import rest.beans.GlobalStatistic;
import settings.Config;
import utils.Utils;

public class SendGlobalStatisticsService extends Thread{

    private final MasterService masterService;
    private volatile boolean quit;

    public SendGlobalStatisticsService(MasterService masterService){ this.masterService = masterService; }

    public void sendGlobalStatistic() {
        GlobalStatistic globalStatistic = masterService.getGlobalStatistic();
        if(globalStatistic != null){
            while(true){
                if(WebServiceClient.getInstance().postGlobalStatistic(masterService.getWorkerDrone().getAdministratorServerAddress(),
                        globalStatistic))
                    break;
            }
        }
    }

    private synchronized void waitTime() throws InterruptedException { wait(Config.TIMEOUT_SEND_GLOBAL_STATS); }

    public void quitService(){ quit = true; }

    @Override
    public void run() {
        String outputHeader = "SEND GLOBAL STATISTICS SERVICE:";
        try {
            while(!quit){
                sendGlobalStatistic();
                waitTime();
            }
            sendGlobalStatistic();
        }catch(InterruptedException e) {
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader), 1);
            Utils.traceIfTest(t);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
