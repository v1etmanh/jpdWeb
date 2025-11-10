package com.jpd.web.service;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Customer;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.RememberTransform;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j

public class DictionaryService {
@Autowired
   private RememberWordRepository repository;
@Autowired
    private ValidationResources validationResources;

   
 
    public List<RememberWordDto> getDictionary(String email) {
        List<RememberWord> rememberWords = repository.findAllByCustomer_Email(email);
        return rememberWords.stream().map(e->RememberTransform.toRememberWordDto(e)).toList();
    }
    public RememberWordDto addRememberWord(String email,RememberWordDto rememberWordDto) {
            Customer customer = validationResources.validateCustomerExist(email);
            if(customer != null){
                RememberWord rememberWord = RememberTransform.toRememberWord(rememberWordDto);
               
                rememberWord.setCustomer(customer);
                repository.save(rememberWord);
                log.info("success to add new remember word {}", rememberWord.getWord());
                return rememberWordDto;
            }
            return null;
    }
    public RememberWordDto updateRememberWord(String email,RememberWordDto rememberWordDto) {
       
       long id=rememberWordDto.getRwId();
       validate(email,id);
    	RememberWord rememberWord= repository.findById(rememberWordDto.getRwId()).orElseThrow(()-> new RuntimeException("Remember word not found"));
        //so sanh voi ai nguoi dung
          RememberWord re=RememberTransform.toRememberWord(rememberWordDto);
          re.setId(rememberWord.getId());
          re.setVote(rememberWord.getVote());
          this.repository.save(re);
          return rememberWordDto;
    }
    public void deleteRememberWord(String email,long id) {
    	 validate(email,id);
  
        repository.deleteById(id);
    }
    private void validate(String email ,long id) {
    	Customer customer=validationResources.validateCustomerExist(email);
   	 Optional< RememberWord> re=this.repository.findById(id);
   	  if(re.isEmpty())throw new RuntimeException("this id is not exist");
   	if(  re.get().getCustomer().getCustomerId()!=customer.getCustomerId())
   		throw new UnauthorizedException("you do not own this word");
    }
}