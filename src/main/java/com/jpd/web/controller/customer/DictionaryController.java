package com.jpd.web.controller.customer;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.Course;
import com.jpd.web.service.DictionaryService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/customer/dictionary")
public class DictionaryController {
    @Autowired
    DictionaryService dictionaryService;
    @GetMapping("/{email}")
    public ResponseEntity<List<RememberWordDto>> getDictionary(@PathVariable("email") String email){
       List<RememberWordDto> rememberWordDtoList= dictionaryService.getDictionary(email);
        return  ResponseEntity.ok(rememberWordDtoList);
    }
    @PostMapping("/{email}")
    public ResponseEntity<RememberWordDto> addDictionary(@PathVariable("email") String email,@RequestBody RememberWordDto rememberWordDto){
        log.info("Post add new remember word customer {} , remember word :{}",email,rememberWordDto.getDescription());
       RememberWordDto wordDto= dictionaryService.addRememberWord(email,rememberWordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(wordDto);
    }
    @DeleteMapping
    public ResponseEntity<Void> deleteDictionary(@RequestParam("rwId") long id){
        dictionaryService.deleteRememberWord(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping
    public ResponseEntity<RememberWordDto> updateDictionary(@RequestBody RememberWordDto rememberWordDto){
       RememberWordDto update = dictionaryService.updateRememberWord(rememberWordDto);
        return ResponseEntity.ok().body(update);
    }


}
