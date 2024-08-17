package com.no1.book.dao.board;

import com.no1.book.common.util.board.BoardPageHandler;
import com.no1.book.common.util.board.BoardSearchCondition;
import com.no1.book.domain.board.BoardFAQDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardFAQDao {
    // faq 검색 개수
    int searchFAQCount(BoardSearchCondition sc);

    // faq 목록 페이징
    List<BoardFAQDto> searchFAQPage(BoardPageHandler ph);

    // faq 삭제
    int deleteFAQ(int faqNum);

    // faq 수정
    int updateFAQ(BoardFAQDto boardFAQDto);

    // faq 등록
    int insertFAQ(BoardFAQDto boardFAQDto);

    // 테스트 용
    // 전체 개수
    int count();
    // 전체 삭제
    int deleteAll();

    // 전체 목록 조회
    List<BoardFAQDto> selectFAQAll();
}