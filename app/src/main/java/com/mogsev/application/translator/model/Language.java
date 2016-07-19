package com.mogsev.application.translator.model;

import java.util.List;

/**
 * Created by zhenya on 19.07.2016.
 */
public class Language {
    public enum TRANSLATE {
        EN_RU,
        RU_EN
    }

    private String code;
    private String name;
    private List<String> listCode;
    private List<String> listName;

    public Language(List listCode, List listName) {
        this.listCode = listCode;
        this.listName = listName;
    }

    public String getCode(int position) {
        return listCode.get(position);
    }

    public String getName(int position) {
        return listName.get(position);
    }
}
