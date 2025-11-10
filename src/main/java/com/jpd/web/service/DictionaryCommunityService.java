package com.jpd.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictionaryCommunityService {
    
    @Autowired
    private RememberWordRepository rememberWordRepository;
    
    /**
     * Lấy tất cả từ vựng với phân trang, sắp xếp theo số vote giảm dần
     */
    public Page<RememberWordDto> findAllWords(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.DESC, "id"));
        
        return rememberWordRepository.findAll(pageRequest)
            .map(this::convertToDto);
    }
    
    /**
     * Tìm kiếm từ vựng theo keyword (tìm trong word, meaning, description)
     */
    public Page<RememberWordDto> searchWords(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        
        Page<RememberWord> results = rememberWordRepository
            .searchRememberWords(
                keyword, pageRequest);
        
        return results.map(this::convertToDto);
    }
    
    /**
     * Lấy chi tiết 1 từ vựng theo ID
     */
    public RememberWordDto getWordById(Long id) {
        RememberWord word = rememberWordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng với ID: " + id));
        
        return convertToDto(word);
    }
    
    /**
     * Vote cho 1 từ vựng (mỗi user chỉ vote 1 lần)
     * @param wordId ID của từ vựng
     * @param customerId ID của người dùng đang vote
     * @return true nếu vote thành công, false nếu đã vote trước đó
     */
    @Transactional
    public boolean voteWord(Long wordId, Long customerId) {
        RememberWord word = rememberWordRepository.findById(wordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng với ID: " + wordId));
        
        // Kiểm tra xem user đã vote chưa
        if (word.getVote().contains(customerId)) {
            return false; // Đã vote rồi
        }
        
        // Thêm vote
        word.getVote().add(customerId);
        rememberWordRepository.save(word);
        
        return true;
    }
    
    /**
     * Hủy vote cho 1 từ vựng
     */
    @Transactional
    public boolean unvoteWord(Long wordId, Long customerId) {
        RememberWord word = rememberWordRepository.findById(wordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng với ID: " + wordId));
        
        boolean removed = word.getVote().remove(customerId);
        
        if (removed) {
            rememberWordRepository.save(word);
        }
        
        return removed;
    }
    
    /**
     * Lấy các từ vựng được vote nhiều nhất (Top words)
     */
    public List<RememberWordDto> getTopVotedWords(int limit) {
        List<RememberWord> allWords = rememberWordRepository.findAll();
        
        return allWords.stream()
            .sorted((w1, w2) -> Integer.compare(
                w2.getVote().size(), 
                w1.getVote().size()))
            .limit(limit)
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra user đã vote cho từ này chưa
     */
    public boolean hasUserVoted(Long wordId, Integer customerId) {
        RememberWord word = rememberWordRepository.findById(wordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng với ID: " + wordId));
        
        return word.getVote().contains(customerId);
    }
    
    /**
     * Convert RememberWord entity sang DTO
     */
    private RememberWordDto convertToDto(RememberWord word) {
        return RememberWordDto.builder()
            .rwId(word.getId())
            .word(word.getWord())
            .meaning(word.getMeaning())
            .description(word.getDescription())
            .synonyms(word.getSynonyms())
            .example(word.getExample())
            .voteCount(word.getVote().size())
            .customerEmail(word.getCustomer().getEmail())
            .build();
    }
}
