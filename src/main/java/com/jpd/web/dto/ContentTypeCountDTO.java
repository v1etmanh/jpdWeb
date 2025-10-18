// Đặt trong package dto của bạn, ví dụ: com.jpd.web.dto
package com.jpd.web.dto;

import com.jpd.web.model.TypeOfContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object để chứa kết quả đếm số lượng content theo từng loại.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentTypeCountDTO {

    private TypeOfContent typeOfContent;
    private long numberOfTypeOfContent;

}