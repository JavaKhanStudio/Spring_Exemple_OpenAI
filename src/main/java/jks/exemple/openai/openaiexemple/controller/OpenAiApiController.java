package jks.exemple.openai.openaiexemple.controller;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping(path = "/openAI/api")
public class OpenAiApiController {


    private final OpenAiChatModel chatModel;

    private final OpenAiImageModel openaiImageModel ;


    @Autowired
    public OpenAiApiController(OpenAiChatModel chatModel, OpenAiImageModel openaiImageModel) {
        this.chatModel = chatModel;
        this.openaiImageModel = openaiImageModel;
    }

    @GetMapping("/message")
    public String getChatResponse(@RequestParam String prompt) {

        ChatResponse response = chatModel.call(
                new Prompt(
                        prompt,
                        OpenAiChatOptions.builder()
                                .model("gpt-4o-mini-2024-07-18")
                                .temperature(0.4)
                                .build()
                ));

        Generation generation = response.getResult() ;
        return generation.getOutput().getText() ;
    }

    @GetMapping("/image")
    public String getImage(@RequestParam String prompt) throws URISyntaxException, IOException {
        ImageResponse response = openaiImageModel.call(
                new ImagePrompt("A light cream colored mini golden doodle",
                        OpenAiImageOptions.builder()
                                .N(1)
                                .model("dall-e-2")
                                .height(1024)
                                .width(1024).build())
        );

        String simplifyName = "../image/" + prompt
                .substring(0, Math.min(prompt.length(), 20))
                .toLowerCase()
                .replace(",","")
                .replace("'","")
                .replace(" ","_")
                + ".png" ;

        Image image = response.getResult().getOutput() ;

        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(image.getUrl());
        RequestEntity<Void> request = RequestEntity.get(uri).build();
        ResponseEntity<byte[]> imageResponse = restTemplate.exchange(request, byte[].class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        byte[] imageBytes = imageResponse.getBody() ;

        Path targetFilePath = Paths.get(simplifyName) ;
        Files.createDirectories(targetFilePath.getParent());

        try (FileOutputStream fos = new FileOutputStream(targetFilePath.toFile())) {
            fos.write(imageBytes);
            System.out.println("Image saved With name " + simplifyName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Image saved with name : " + simplifyName + " \nURL: " + image.getUrl() ;
    }

}
