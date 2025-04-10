package jks.exemple.openai.openaiexemple.controller;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
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
import reactor.core.publisher.Mono;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping(path = "/openAI/api")
public class OpenAiApiController {

    // Models List
    // https://platform.openai.com/docs/models

    private final String audioFilesPath = "../output/audioFile/" ;
    private final String imageFilesPath = "../output/image/" ;
    private final OpenAiChatModel chatModel;

    private final OpenAiImageModel openaiImageModel ;

    private final OpenAiAudioSpeechModel audioSpeechModel;

    @Autowired
    public OpenAiApiController(OpenAiChatModel chatModel, OpenAiImageModel openaiImageModel, OpenAiAudioSpeechModel audioSpeechModel) {
        this.chatModel = chatModel;
        this.openaiImageModel = openaiImageModel;
        this.audioSpeechModel = audioSpeechModel;
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

        String simplifyName = imageFilesPath + prompt
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

    @GetMapping("/voice")
    public byte[] getVoice(@RequestParam String prompt) throws URISyntaxException, IOException {
        byte[] audio = audioSpeechModel.call("This is a test") ;

        String simplifyName = prompt
                .substring(0, Math.min(prompt.length(), 20))
                .toLowerCase()
                .replace(",","")
                .replace("'","")
                .replace(" ","_")
                + ".mp3" ;

        Path targetFilePath = Paths.get(audioFilesPath + simplifyName) ;
        Files.createDirectories(targetFilePath.getParent());
        Files.write(targetFilePath, audio);

        return audio ;
    }

    // https://platform.openai.com/docs/api-reference/introduction
    @GetMapping("/voice/adv")
    public byte[] getVoiceAdv(@RequestParam String prompt) throws URISyntaxException, IOException {
        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions
                .builder()
                .model("tts-1-hd")
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ONYX)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(prompt, options) ;
        SpeechResponse speech = audioSpeechModel.call(speechPrompt) ;
        byte[] audio = speech.getResult().getOutput();
        String simplifyName = prompt
                .substring(0, Math.min(prompt.length(), 20))
                .toLowerCase()
                .replace(",","")
                .replace("'","")
                .replace(" ","_")
                + ".mp3" ;

        Path targetFilePath = Paths.get(audioFilesPath + simplifyName) ;
        Files.createDirectories(targetFilePath.getParent());
        Files.write(targetFilePath, audio);

        return audio ;
    }

}
