package utils;

import settings.Config;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Random;

public class Utils {

    public static int readIntegerDefault(String requestMessage, String errorMessage, int lowerInt, int upperInt) throws IOException {
        int ret = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int defaultInt = new Random().nextInt(upperInt - lowerInt) + lowerInt;
        boolean ok = false;
        do {
            System.out.print(String.format("%s (default-> %d): ", requestMessage, defaultInt));
            String sRet = br.readLine().trim();
            try {
                if (!sRet.equals("")) {
                    ret = Integer.parseInt(sRet);
                } else {
                    ret = defaultInt;
                }
                ok = true;
            } catch (NumberFormatException ex) {
                System.out.println(errorMessage);
            }
        } while (!ok);
        return ret;
    }

    public static int readInt() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("\nInsert an integer number: ");
        int n;
        while(true){
            try{
                n = Integer.parseInt(br.readLine().trim());
                break;
            }catch(NumberFormatException e){
                System.out.println("\nPlease, insert an integer number");
            }
        }
        return  n;
    }

    public static long readLong() throws IOException {
        System.out.print("\nInsert a long number: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        long n;
        while(true){
            try{
                n = Long.parseLong(br.readLine().trim());
                break;
            }catch(NumberFormatException e){
                System.out.println("\nPlease, insert a long number");
            }
        }
        return  n;
    }

    public static void printIfLevel(String s, int level){
        if(level <= Config.PRINT_LEVEL){
            System.out.println(s);
        }
    }

    public static void traceIfTest(Throwable t){
        if(Config.PRINT_LEVEL >= 2){
            t.printStackTrace();
        }
    }

}
