package com.mhkj.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Api("测试组")
public class TestController {

    @ApiOperation(value = "测试", notes = "测试")
    @GetMapping("/testGet")
    public String testGet() {
        return "测试成功";
    }

}
