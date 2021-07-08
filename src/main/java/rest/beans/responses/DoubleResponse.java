package rest.beans.responses;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DoubleResponse {

    private double value;

    public DoubleResponse(){}

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}