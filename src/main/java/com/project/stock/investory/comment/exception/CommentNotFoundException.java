package com.project.stock.investory.comment.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends BusinessException {
    public CommentNotFoundException () {
        super("댓글이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
}
