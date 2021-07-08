package domain.drone.services;

import domain.drone.WorkerDrone;
import domain.sensor.MeasurementBuffer;
import domain.sensor.simulator.Measurement;
import domain.sensor.simulator.PM10Simulator;
import utils.Utils;

import java.util.List;

public class SensorService extends Thread{

    private final WorkerDrone workerDrone;
    private final MeasurementBuffer measurementBuffer;
    private final PM10Simulator pm10Simulator;
    private volatile boolean quit;

    public SensorService(WorkerDrone workerDrone, MeasurementBuffer measurementBuffer){
        this.workerDrone = workerDrone;
        this.measurementBuffer = measurementBuffer;
        this.pm10Simulator = new PM10Simulator(measurementBuffer);
    }

    public void quitService(){ quit = true; }

    public double calculateMean(List<Measurement> measurementList){
        double sum = 0;
        for (Measurement m: measurementList) {
            sum += m.getValue();
        }
        return  sum / measurementList.size();
    }

    @Override
    public void run() {
        String outputHeader = "SENSOR SERVICE:";
        try{
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2 );
            pm10Simulator.start();
            while(!quit){
                double mean = calculateMean(measurementBuffer.readAllAndClean());
                workerDrone.addMeasurementMean(mean);
            }
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
