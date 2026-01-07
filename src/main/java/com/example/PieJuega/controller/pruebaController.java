package com.example.PieJuega.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class pruebaController {

    @GetMapping("/hola")
    public String hola(){
        return "hola que tal";
    }
}
