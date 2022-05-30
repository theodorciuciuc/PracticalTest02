package ro.pub.cs.systems.eim.practicaltest02;

import java.util.Date;

public class ValueClass {
    public String Value;
    public Date date;

    public ValueClass(String rate)
    {
        this.Value = rate;
        date = new Date();
    }

    @Override
    public String toString() {
        return "ValueClass{" +
                "rate='" + Value + '\''+
                '}';
    }
}
