package com.reme.re_me;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
   public HelloController() {
   }

   @GetMapping({"/api/hello"})
   public String hello() {
      return "Hello, App Backend!";
   }
}
