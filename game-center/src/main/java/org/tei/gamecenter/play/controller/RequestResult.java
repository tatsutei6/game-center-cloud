package org.tei.gamecenter.play.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestResult<T> {
    private int code;
    private String message;
    private T data;
}
