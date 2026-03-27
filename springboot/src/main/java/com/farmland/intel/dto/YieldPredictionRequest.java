package com.farmland.intel.dto;

/**
 * 产量预测请求DTO
 * 用于接收前端传入的作物种植信息，进行产量预测
 */
public class YieldPredictionRequest {
    private String cropType;
    private double plantingArea;
    private double averageYield;
    private double fertilizerUsage;
    private double irrigationAmount;
    private double temperature;
    private double humidity;
    private String growthStage;

    public YieldPredictionRequest() {
    }

    public YieldPredictionRequest(String cropType, double plantingArea, double averageYield,
                                double fertilizerUsage, double irrigationAmount,
                                double temperature, double humidity, String growthStage) {
        this.cropType = cropType;
        this.plantingArea = plantingArea;
        this.averageYield = averageYield;
        this.fertilizerUsage = fertilizerUsage;
        this.irrigationAmount = irrigationAmount;
        this.temperature = temperature;
        this.humidity = humidity;
        this.growthStage = growthStage;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public double getPlantingArea() {
        return plantingArea;
    }

    public void setPlantingArea(double plantingArea) {
        this.plantingArea = plantingArea;
    }

    public double getAverageYield() {
        return averageYield;
    }

    public void setAverageYield(double averageYield) {
        this.averageYield = averageYield;
    }

    public double getFertilizerUsage() {
        return fertilizerUsage;
    }

    public void setFertilizerUsage(double fertilizerUsage) {
        this.fertilizerUsage = fertilizerUsage;
    }

    public double getIrrigationAmount() {
        return irrigationAmount;
    }

    public void setIrrigationAmount(double irrigationAmount) {
        this.irrigationAmount = irrigationAmount;
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

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    @Override
    public String toString() {
        return "YieldPredictionRequest{" +
                "cropType='" + cropType + '\'' +
                ", plantingArea=" + plantingArea +
                ", averageYield=" + averageYield +
                ", fertilizerUsage=" + fertilizerUsage +
                ", irrigationAmount=" + irrigationAmount +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", growthStage='" + growthStage + '\'' +
                '}';
    }
}
