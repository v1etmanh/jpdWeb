package com.jpd.web.controller.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.Customer;
import com.jpd.web.service.DictionaryCommunityService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.service.CustomerService;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryCommunityController {
    
    @Autowired
    private DictionaryCommunityService dictionaryService;
    
    @Autowired
    private ValidationResources  validationResources;
    
    /**
     * Lấy tất cả từ vựng với phân trang
     * GET /api/dictionary/words?page=0&size=20
     */
    @GetMapping("/words")
    public ResponseEntity<Map<String, Object>> getAllWords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<RememberWordDto> wordsPage = dictionaryService.findAllWords(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("words", wordsPage.getContent());
            response.put("currentPage", wordsPage.getNumber());
            response.put("totalItems", wordsPage.getTotalElements());
            response.put("totalPages", wordsPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Tìm kiếm từ vựng theo keyword
     * GET /api/dictionary/search?keyword=hello&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchWords(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
        	
            Page<RememberWordDto> wordsPage = dictionaryService.searchWords(keyword, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("words", wordsPage.getContent());
            response.put("currentPage", wordsPage.getNumber());
            response.put("totalItems", wordsPage.getTotalElements());
            response.put("totalPages", wordsPage.getTotalPages());
            response.put("keyword", keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
        	System.out.print(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lấy chi tiết 1 từ vựng
     * GET /api/dictionary/words/{id}
     */
    @GetMapping("/words/{id}")
    public ResponseEntity<?> getWordById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt ) {
        try {
            RememberWordDto word = dictionaryService.getWordById(id);
            String email=jwt.getClaimAsString("email");
           
           
                
                Map<String, Object> response = new HashMap<>();
                response.put("word", word);
                
            
            
            
            return ResponseEntity.ok(word);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Vote cho 1 từ vựng
     * POST /api/dictionary/words/{id}/vote
     */
    @PostMapping("/words/{id}/vote")
    public ResponseEntity<Map<String, Object>> voteWord(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
       
        
        try {
            String email = jwt.getClaimAsString("email");
            Customer customer = validationResources.validateCustomerExist(email);
            
            boolean success = dictionaryService.voteWord(id, customer.getCustomerId());
            
            if (success) {
                RememberWordDto updatedWord = dictionaryService.getWordById(id);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vote thành công",
                    "voteCount", updatedWord.getVoteCount()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", "Bạn đã vote cho từ này rồi"
                    ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Hủy vote cho 1 từ vựng
     * DELETE /api/dictionary/words/{id}/vote
     */
    @DeleteMapping("/words/{id}/vote")
    public ResponseEntity<Map<String, Object>> unvoteWord(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        
        
        try {
            String email = jwt.getClaimAsString("email");
            Customer customer = validationResources.validateCustomerExist(email);
            
            boolean success = dictionaryService.unvoteWord(id, customer.getCustomerId());
            
            if (success) {
                RememberWordDto updatedWord = dictionaryService.getWordById(id);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Hủy vote thành công",
                    "voteCount", updatedWord.getVoteCount()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", "Bạn chưa vote cho từ này"
                    ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lấy top từ vựng được vote nhiều nhất
     * GET /api/dictionary/top?limit=10
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTopVotedWords(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<RememberWordDto> topWords = dictionaryService.getTopVotedWords(limit);
            return ResponseEntity.ok(topWords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}