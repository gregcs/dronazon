package domain.sensor;

import domain.sensor.simulator.Buffer;
import domain.sensor.simulator.Measurement;

import java.util.ArrayList;
import java.util.List;

public class MeasurementBuffer implements Buffer {
    
    private final List<Measurement> buffer;
    private final int windowsSize;
    private final int windowsOverlap;

    public MeasurementBuffer(int windowsSize, int windowsOverlap){
        buffer = new ArrayList<>();
        this.windowsSize = windowsSize;
        this.windowsOverlap = windowsOverlap;
    }

    private synchronized void add(Measurement measurement) {
        if(buffer.size() >= windowsSize){
            notify();
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        buffer.add(measurement);
    }

    private synchronized List<Measurement> readWindow(){
        while(buffer.size() < windowsSize) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        List<Measurement> window = new ArrayList<>(buffer);
        int overlap = Math.floorDiv((windowsSize * windowsOverlap), 100);
        buffer.removeAll(buffer.subList(0, overlap));
        notify();
        return window;
    }

    @Override
    public void addMeasurement(Measurement m) { add(m); }

    @Override
    public List<Measurement> readAllAndClean() { return readWindow(); }

}
