package com.personal.demo.service.serviceImpl;

import com.personal.demo.service.TestService;
import com.personal.wspring.annotation.WService;

@WService
public class TestServiceImpl implements TestService {
    @Override
    public String listClassName() {
        return "1341234123TextService";
    }
}
