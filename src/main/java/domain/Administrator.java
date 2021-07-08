package domain;

import domain.other.WebServiceClient;
import rest.beans.Drone;
import rest.beans.GlobalStatistic;
import settings.Config;
import utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class Administrator {

    private final WebServiceClient client;
    private final String serverAddress;

    public Administrator(String serverAddress){
        client = WebServiceClient.getInstance();
        this.serverAddress = serverAddress;
    }

    public static void printAvailableServices(){
        System.out.println("\nSERVICES AVAILABLE:\n" +
                "1) List of drones present in the network\n" +
                "2) Latest n global statistics relating to the smart-city\n" +
                "3) Average number of deliveries made by smart-city drones between two timestamps t1 and t2\n" +
                "4) Average of kilometers traveled by smart-city drones between two timestamps t1 and t2\n" +
                "5) Show available services\n" +
                "6) Quit\n");
    }

    private void droneListCase(){
        List<Drone> droneList = client.getDrones(serverAddress);
        if(droneList != null && !droneList.isEmpty()){
            System.out.println("\nLIST OF DRONES:");
            System.out.println(droneList);
        }else{
            System.out.println("\nNO DRONES");
        }
    }

    private void lastNGlobalStatisticsCase() throws IOException {
        int n = Utils.readInt();
        List<GlobalStatistic> lastNGlobalStatistics = client.getLastNGlobalStatistics(serverAddress, n);
        if(lastNGlobalStatistics != null && !(lastNGlobalStatistics.isEmpty())){
            Utils.printIfLevel(String.format("\nLIST OF %d GLOBAL STATISTICS:", n), 0);
            for (GlobalStatistic globalstatistic: lastNGlobalStatistics) {
                System.out.println("-->" + globalstatistic);
            }
        }else{
            System.out.println("\nNO GLOBAL STATISTICS");
        }
    }

    private void averageDeliveriesCase() throws IOException {
        long t1 = Utils.readLong();
        long t2 = Utils.readLong();
        if(t2 >= t1){
            Double averageDeliveries = client.getMeanDeliveriesMade(serverAddress, t1, t2);
            if(averageDeliveries != null && !(averageDeliveries.isNaN())){
                Utils.printIfLevel(String.format("\nAverage deliveries made: %,.2f", averageDeliveries),0);
            }else{
                System.out.println("\nNO GLOBAL STATISTICS IN THIS RANGE");
            }
        }else{
            System.out.println("\nt2 MUST BE GREATER OR EQUAL THAN t1");
        }
    }

    private void averageKmTraveledCase() throws IOException {
        long t1 = Utils.readLong();
        long t2 = Utils.readLong();
        if(t2 >= t1){
            Double averageKmTraveled = client.getMeanKmTraveled(serverAddress, t1, t2);
            if(averageKmTraveled != null && !(averageKmTraveled.isNaN())){
                Utils.printIfLevel(String.format("\nAverage km traveled: %,.2f", averageKmTraveled),0);
            }else{
                System.out.println("\nNO GLOBAL STATISTICS IN THIS RANGE");
            }
        }else{
            System.out.println("\nt2 MUST BE GREATER OR EQUAL THAN t1");
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Administrator administrator = new Administrator(Config.ADMINISTRATOR_SERVER_ADDRESS);
        System.out.println("\nWELCOME ADMINISTRATOR!");
        printAvailableServices();
        boolean stop = false;
        String[] options = {"1", "2", "3", "4", "5", "6"};
        while(!stop){
            System.out.print("\nChoose a service (1-6): ");
            String choose = br.readLine();
            if(!Arrays.asList(options).contains(choose))
                System.out.println("\nUNKNOWN COMMAND!");
            else{
                try {
                    switch (choose){
                        case "1":
                            administrator.droneListCase();
                            break;
                        case "2":
                            administrator.lastNGlobalStatisticsCase();
                            break;
                        case "3":
                            administrator.averageDeliveriesCase();
                            break;
                        case "4":
                            administrator.averageKmTraveledCase();
                            break;
                        case "5":
                            printAvailableServices();
                            break;
                        case "6":
                            stop = true;
                            System.out.println("\nGOODBYE!");
                            break;
                    }
                }catch(Throwable t){
                    System.out.println("\nERROR");
                    t.printStackTrace();
                }
            }
        }
    }

}
