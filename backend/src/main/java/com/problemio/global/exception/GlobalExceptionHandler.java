package com.problemio.global.exception;

import com.problemio.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        // 인증/인가 관련 에러는 적절한 HTTP 상태 코드로 내려준다.
        return switch (errorCode) {
            case LOGIN_REQUIRED -> ResponseEntity
                    .status(401)
                    .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
            case ACCESS_DENIED -> ResponseEntity
                    .status(403)
                    .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
            default -> ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
        };
    }




    // 404 예외 처리 (SPA 지원)
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Object handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        String path = e.getResourcePath();

        // 1. API 요청이거나 파일 확장자가 있는 정적 리소스 요청인 경우 -> 404 JSON 응답
        if (path.startsWith("api/") || path.contains(".")) {
            return ResponseEntity
                    .status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("NOT_FOUND", "Resource not found: " + path));
        }

        // 2. 그 외(SPA 라우트)인 경우 -> index.html로 포워딩 (화면 반환)
        return new org.springframework.web.servlet.ModelAndView("forward:/index.html");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // TODO: 로그 처리
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
