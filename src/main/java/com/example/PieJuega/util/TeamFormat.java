package com.example.PieJuega.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamFormat {
    FIVE(5),
    SEVEN(7),
    EIGHT(8),
    ELEVEN(11);

    private final int playersOnField;
}
