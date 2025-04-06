package jks.exemple.openai.openaiexemple.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping(path = "/openAI/native")
public class OpenAiNativeController {

    private final WebClient webClient;

    public OpenAiNativeController(@Qualifier("openAiWebClient") WebClient openAiWebClient) {
        this.webClient = openAiWebClient;
    }

    @GetMapping("/message")
    public String getChatResponse(@RequestParam String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini-2024-07-18",
                "input", prompt
        );
        try {
            String rawJson = webClient.post()
                    .uri("/v1/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            // Navigate to: output[0] → content[0] → text
            JsonNode textNode = root.path("output")
                    .path(0)
                    .path("content")
                    .path(0)
                    .path("text");

            String message = textNode.asText();

            return message != null && !message.isEmpty() ? message : "No assistant reply found.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // Usage does not work with an API... Cause
    /*
    @GetMapping("/usage")
    public String getUsage(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        String startDate = (start != null) ? start : LocalDate.now().withDayOfMonth(1).toString();
        String endDate = (end != null) ? end : LocalDate.now().toString();

        try {
            Mono<String> usageQuery = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/dashboard/billing/usage")
                            .queryParam("start_date", startDate)
                            .queryParam("end_date", endDate)
                            .build()
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Client error: " + errorBody)))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Server error: " + errorBody)))
                    )
                    .bodyToMono(String.class);


            String value = usageQuery.block();
            System.out.println("Usage response: " + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    */

}
