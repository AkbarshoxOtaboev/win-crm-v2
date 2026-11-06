package uz.script.wincrm.goods.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {

    PRODUCT("Tovar"),
    SERVICE("Xizmat"),
    WINDOW("Oyna"),
    FINISHED_PRODUCT("Tayyor mahsulot");

    private final String label;
}
