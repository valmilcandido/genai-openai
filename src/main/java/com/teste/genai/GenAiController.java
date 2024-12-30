package com.teste.genai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GenAiController {
    private final ChatClient chatClient;
    private final OpenAiImageModel imageModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    GenAiController(ChatClient chatClient, 
                    OpenAiImageModel imageModel, 
                    OpenAiAudioTranscriptionModel transcriptionModel,
                    OpenAiAudioSpeechModel openAiAudioSpeechModel){
        this.chatClient = chatClient;
        this.imageModel = imageModel;
        this.transcriptionModel = transcriptionModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }

    @GetMapping("/prompt")
    public String prompt(@RequestParam String message){
        return chatClient.prompt().user(message).call().content();
    }

    @GetMapping("/image")
    public String image(@RequestParam String message){

        ImageResponse response = imageModel.call(
            new ImagePrompt(message,
            OpenAiImageOptions.builder()
                    .withQuality("hd")
                    .withN(1)
                    .withHeight(1024)
                    .withWidth(1024).build())

        );

        System.out.println("imagem gerada");

        return response.getResult().getOutput().getUrl();
    }

    @GetMapping("/audio")
    public String audio(){

        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
            .withLanguage("en")
            .withPrompt("Ask not this, but ask that")
            .withTemperature(0f)
            .withResponseFormat(TranscriptResponseFormat.TEXT)
            .build();

        var audioFile = new FileSystemResource("src\\main\\resources\\static\\audio.mp3");    

        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = transcriptionModel.call(transcriptionRequest);
        return response.getResult().getOutput().toLowerCase();
    }

    @GetMapping("/speech")
    public String speech(@RequestParam String message){
        
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
            .withModel("tts-1")
            .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
            .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
            .withSpeed(1.0f)
            .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(message, speechOptions);
        SpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);
        byte[] responseAsBytes = response.getResult().getOutput();

        try{

            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

            String dataFormatada = LocalDateTime.now().format(formatador);

            File arquivoFile = new File("src\\main\\resources\\static\\"+dataFormatada+".mp3");
            arquivoFile.createNewFile();
            Path caminho = Paths.get(arquivoFile.getAbsolutePath());
            Files.write(caminho, responseAsBytes);
        }catch(IOException ex){
            System.out.println(ex);
        }
        
        return "Arquivo gerado com sucesso";
    }

}
