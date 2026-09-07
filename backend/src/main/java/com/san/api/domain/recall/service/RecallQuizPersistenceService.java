package com.san.api.domain.recall.service;

import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.repository.RecallQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 리콜 퀴즈 영속화 Service */
@Service
@RequiredArgsConstructor
public class RecallQuizPersistenceService {

    private final RecallQuizRepository recallQuizRepository;

    /**
     * 리콜 퀴즈 저장
     *
     * @param quizzes 저장할 리콜 퀴즈 목록
     * @return 저장된 리콜 퀴즈 목록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<RecallQuiz> saveQuizzes(List<RecallQuiz> quizzes) {
        return recallQuizRepository.saveAllAndFlush(quizzes);
    }
}
