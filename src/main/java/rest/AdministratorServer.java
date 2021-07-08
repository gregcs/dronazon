package rest;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import settings.Config;
import utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdministratorServer {

    public static void main(String[] args) throws IOException {
        String administratorServerAddress = Config.ADMINISTRATOR_SERVER_ADDRESS;

        HttpServer server = HttpServerFactory.create(administratorServerAddress);
        server.start();
        Utils.printIfLevel(String.format("\nADMINISTRATOR SERVER STARTED ON %s.\n", administratorServerAddress),0);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        do{
            Utils.printIfLevel("\nENTER \"quit\" TO STOP THE ADMINISTRATOR SERVER.\n",0);
        }while (!br.readLine().equals("quit"));

        Utils.printIfLevel("\nSTOPPING ADMINISTRATOR SERVER...\n",0);
        server.stop(0);
        Utils.printIfLevel("\nADMINISTRATOR SERVER STOPPED.\n",0);
    }

}