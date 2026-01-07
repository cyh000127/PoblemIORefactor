package com.problemio.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED("U002", "이미 가입된 이메일입니다."),
    EMAIL_NOT_VERIFIED("U005", "이메일 인증이 필요합니다."),
    NICKNAME_DUPLICATED("U003", "이미 사용 중인 닉네임입니다."),
    INVALID_LOGIN("U004", "이메일 또는 비밀번호가 올바르지 않습니다."),
    QUIZ_NOT_FOUND("Q001", "퀴즈를 찾을 수 없습니다."),
    QUESTION_NOT_FOUND("Q002", "문제를 찾을 수 없습니다."),
    COMMENT_NOT_FOUND("C001", "댓글을 찾을 수 없습니다."),
    COMMENT_CONTENT_REQUIRED("C002", "댓글 내용을 입력하세요."),
    COMMENT_NICKNAME_REQUIRED("C003", "닉네임을 입력하세요."),
    COMMENT_PASSWORD_REQUIRED("C004", "비밀번호를 입력하세요."),
    COMMENT_PASSWORD_MISMATCH("C005", "비밀번호가 일치하지 않습니다."),
    COMMENT_FORBIDDEN("C006", "본인 댓글만 수정/삭제할 수 있습니다."),
    LOGIN_REQUIRED("A002", "로그인 후 이용해주세요."),
    ACCESS_DENIED("A001", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR("S001", "서버 내부 오류가 발생했습니다."),
    CANNOT_LIKE_OWN_QUIZ("Q003", "자신이 만든 퀴즈에는 좋아요를 누를 수 없습니다."),
    INVALID_FILE_TYPE("F001", "지원하지 않는 파일 형식입니다."),
    INVALID_INPUT_VALUE("G001", "잘못된 입력값입니다.");


    private final String code;
    private final String message;
}
