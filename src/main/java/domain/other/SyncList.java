package domain.other;

import java.util.ArrayList;
import java.util.List;

public class SyncList <T>{

    private List<T> list;

    public SyncList(){
        list = new ArrayList<>();
    }

    public SyncList(List<T> list) {
        this.list = list;
    }

    public synchronized void add(T element){
        list.add(element);
    }

    public synchronized List<T> getAll(){
        return new ArrayList<>(list);
    }

    public synchronized void remove(T element){
        if(element != null)
            list.remove(element);
    }

    public synchronized void setList(List<T> list){ this.list = list; }

    public synchronized List<T> removeAll(){
        List<T> ret = getAll();
        list.removeAll(ret);
        return ret;
    }

}
