package com.personal.demo.controller;

import com.personal.demo.service.TestService;
import com.personal.wspring.annotation.WAutowired;
import com.personal.wspring.annotation.WController;
import com.personal.wspring.annotation.WRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WRequestMapping("/test")
@WController
public class TestController {

    @WAutowired
    TestService testService;

    @WRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response){
        if (request.getParameter("name") == null){
            try {
                response.getWriter().write("name is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String name = request.getParameter("name");
            try {
                response.getWriter().write("name is [" + name + "]");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @WRequestMapping("/listClassName")
    public void listClassName(HttpServletRequest req, HttpServletResponse resp) {
        String str = testService.listClassName();
        System.out.println("testXService----------=-=-=>" + str);
        try {
            resp.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
