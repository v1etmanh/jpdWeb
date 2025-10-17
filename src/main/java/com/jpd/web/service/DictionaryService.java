package com.jpd.web.service;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.model.Customer;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.transform.RememberTransform;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DictionaryService {

    RememberWordRepository repository;

    CustomerService customerService;

    RememberTransform transform;
    public DictionaryService(RememberWordRepository repository,
                             CustomerService customerService,
                             RememberTransform transform) {
        this.repository = repository;
        this.customerService = customerService;
        this.transform = transform;
    }
    public List<RememberWordDto> getDictionary(String email) {
        List<RememberWord> rememberWords = repository.findAllByCustomer_Email(email);
        return rememberWords.stream().map(transform::toRememberWordDto).toList();
    }
    public RememberWordDto addRememberWord(String email,RememberWordDto rememberWordDto) {
            Customer customer = customerService.getOrCreateAccount(email);
            if(customer != null){
                RememberWord rememberWord = transform.toRememberWord(rememberWordDto);
                rememberWord.setCustomer(customer);
                repository.save(rememberWord);
                log.info("success to add new remember word {}", rememberWord.getWord());
                return rememberWordDto;
            }
            return null;
    }
    public RememberWordDto updateRememberWord(RememberWordDto rememberWordDto) {
        RememberWord rememberWord= repository.findById(rememberWordDto.getRwId()).orElseThrow(()-> new RuntimeException("Remember word not found"));
        transform.UpdateRememberWord(rememberWord,rememberWordDto);
        return  transform.toRememberWordDto(repository.save(rememberWord));
    }
    public void deleteRememberWord(long id) {
        repository.deleteById(id);
    }
}
