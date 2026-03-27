package com.farmland.intel.controller;

public class YieldPredictionRequest {
    private String cropType;
    private double plantingArea;
    private double averageYield;
    private double fertilizerUsage;
    private double irrigationAmount;
    private double temperature;
    private double humidity;
    private String growthStage;

    // getters and setters
    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public double getPlantingArea() { return plantingArea; }
    public void setPlantingArea(double plantingArea) { this.plantingArea = plantingArea; }
    public double getAverageYield() { return averageYield; }
    public void setAverageYield(double averageYield) { this.averageYield = averageYield; }
    public double getFertilizerUsage() { return fertilizerUsage; }
    public void setFertilizerUsage(double fertilizerUsage) { this.fertilizerUsage = fertilizerUsage; }
    public double getIrrigationAmount() { return irrigationAmount; }
    public void setIrrigationAmount(double irrigationAmount) { this.irrigationAmount = irrigationAmount; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public String getGrowthStage() { return growthStage; }
    public void setGrowthStage(String growthStage) { this.growthStage = growthStage; }
}