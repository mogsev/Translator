package com.mogsev.application.translator.model;

/**
 * Created by zhenya on 19.07.2016.
 */
public class TranslateItem {
    private int id;
    private String strIn;
    private String strOut;

    public TranslateItem() {

    }

    public TranslateItem(String strIn, String strOut) {
        this.strIn = strIn;
        this.strOut = strOut;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setIn(String strIn) {
        this.strIn = strIn;
    }

    public void setOut(String strOut) {
        this.strOut = strOut;
    }

    public String getIn() {
        return strIn;
    }

    public String getOut() {
        return strOut;
    }

    @Override
    public String toString() {
        return "TranslateItem{" +
                "id=" + id +
                ", strIn='" + strIn + '\'' +
                ", strOut='" + strOut + '\'' +
                '}';
    }
}
