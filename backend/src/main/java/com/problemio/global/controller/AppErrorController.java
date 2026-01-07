package com.problemio.global.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                 // 404 발생 시
                 // API 요청(/api/...)이나 정적 리소스(.js, .css, .png 등)가 아니면 index.html로 포워딩
                 String path = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
                 
                 // path가 null일 수도 있음
                 if (path == null) {
                     return "forward:/index.html";
                 }

                 // API 요청은 그대로 404 (또는 GlobalExceptionHandler가 처리)
                 if (path.startsWith("/api/")) {
                     return "error/404"; // 또는 null 반환하여 기본 에러 처리
                 }
                 
                 // 확장자가 있는 파일 요청도 그대로 404
                 // 간단히 마지막 . 뒤에 글자가 있으면 파일로 간주 (단, 경로는 /로 구분)
                 String fileName = path.substring(path.lastIndexOf("/") + 1);
                 if (fileName.contains(".")) {
                     return "error/404";
                 }

                 // 그 외(SPA 라우트)는 index.html로
                 return "forward:/index.html";
            }
        }
        return "error";
    }
}
