package Main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.*;
import java.util.Arrays;
import java.util.List;

public class Weather {

    private static List<String> availableLanguages = Arrays.asList("en", "ru", "it", "sp", "ua", "de", "pt", "ro", "pl", "fi", "nl", "fr", "bg", "se", "zh_tw", "zh_cn", "tr");
    private static String getLanguage(String language){
        language = language.toLowerCase();
        if(availableLanguages.contains(language))
            return language;
        else
            return "en";
    }

    public static JsonNode getWeather(String city, String language, boolean metric){
        language = getLanguage(language);
        try {
            String url = "https://community-open-weather-map.p.rapidapi.com/weather?q=" + URLEncoder.encode(city, "UTF-8") + "&units=" + (metric ? "metric" : "imperial") + "&lang=" + language;
            URL queryURL = new URL(url);
            Connection connection = Jsoup.connect(queryURL.toExternalForm())
                    .header("X-RapidAPI-Key", Tokens.WEATHER_KEY)
                    .header("X-RapidAPI-Host", "community-open-weather-map.p.rapidapi.com")
                    .ignoreContentType(true);
            Document document = connection.get();
            return new ObjectMapper().readTree(document.text());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getImageUrl(String image){
        return "http://openweathermap.org/img/w/" + image + ".png";
    }

}
