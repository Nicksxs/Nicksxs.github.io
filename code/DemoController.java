package com.nicksxs.spbdemo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.ParserConfig;
import com.nicksxs.spbdemo.Domain.DemoResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author shixuesen
 * @date 2021/7/23
 */
@RestController
public class DemoController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public DemoResponse test() {
        String item = "{\"id\": 1, \"name\": \"nick\"}";
        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
        DemoResponse response = JSON.parseObject(item, DemoResponse.class, parserConfig);
        return response;
    }

    @RequestMapping(value = "request1", method = RequestMethod.GET)
    public void request1(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping(value = "request2", method = RequestMethod.GET)
    public void request2(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Location", "https://www.baidu.com");
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }


    @RequestMapping(value = "request3", method = RequestMethod.GET)
    public void request3(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Location", "https://www.baidu.com");
//        Cookie cookie = new Cookie("a", "b");
//        cookie.setSecure(true);
//        cookie.setDomain("baidu.com");
//        response.addCookie(cookie);
        response.addHeader("Set-Cookie", "a=b; domain=*.baidu.com; SameSite=none;Secure");
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }


}
