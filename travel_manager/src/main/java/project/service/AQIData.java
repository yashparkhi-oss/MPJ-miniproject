package project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AQIData {
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private int aqi;
        public int getAqi() { return aqi; }
        public void setAqi(int aqi) { this.aqi = aqi; }
    }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}