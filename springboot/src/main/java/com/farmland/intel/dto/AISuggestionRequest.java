package com.farmland.intel.dto;

/**
 * AI建议请求DTO
 * 用于接收前端传入的农业环境参数，生成AI智能建议
 */
public class AISuggestionRequest {
    private String cropType;
    private double temperature;
    private double humidity;
    private double ph;
    private String growthStage;
    private String problemDescription;

    public AISuggestionRequest() {
    }

    public AISuggestionRequest(String cropType, double temperature, double humidity,
                              double ph, String growthStage, String problemDescription) {
        this.cropType = cropType;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ph = ph;
        this.growthStage = growthStage;
        this.problemDescription = problemDescription;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getPh() {
        return ph;
    }

    public void setPh(double ph) {
        this.ph = ph;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    @Override
    public String toString() {
        return "AISuggestionRequest{" +
                "cropType='" + cropType + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", ph=" + ph +
                ", growthStage='" + growthStage + '\'' +
                ", problemDescription='" + problemDescription + '\'' +
                '}';
    }
}
