package com.jpd.web.model;


import java.util.List;

public class BlacklistWord {
    private String word;
    private double weight;
    private String category;
    private List<String> variants;
    private boolean context_safe;
    private List<String> examples;
    
    // Getters & Setters
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    
    public List<String> getVariants() { return variants; }
    public void setVariants(List<String> variants) { this.variants = variants; }
}