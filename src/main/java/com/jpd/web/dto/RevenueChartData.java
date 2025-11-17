package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueChartData {
    private List<String> labels; // ["Tuần 1", "Tuần 2",...] hoặc ["Jan", "Feb",...]
    private List<Double> data; // [1000, 1500, 2000,...]
    private String period; // "WEEK", "MONTH", "YEAR"
}