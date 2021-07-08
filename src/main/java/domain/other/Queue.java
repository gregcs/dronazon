package domain.other;

import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Queue<T> {

    public List<T> buffer = new ArrayList<>();

    public synchronized void put(T element) {
        buffer.add(element);
        notify();
    }

    public synchronized T take() {
        T element;

        while(buffer.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        element = buffer.get(0);
        buffer.remove(0);

        return element;
    }

    public synchronized T top(){
        try{
            return buffer.get(0);
        }catch(IndexOutOfBoundsException ex){
            return null;
        }
    }

    public synchronized void waitIfNotEmptyQueue() throws InterruptedException {
        Utils.printIfLevel("QUEUE LENGTH " + length(), 1);
        if(top() != null){
            wait();
        }
    }

    public synchronized T takeAndNotifyIfEmpty(){
        T ret = take();
        if(top() == null){
            notifyAll();
        }
        return ret;
    }

    public synchronized int length() {
        return buffer.size();
    }


}
