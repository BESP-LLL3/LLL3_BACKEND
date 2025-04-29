package com.sangchu.branding.entity;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private String serviceTier;

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String logprobs;
        private String finishReason;

        @Data
        public static class Message {
            private String role;
            private String content;
            private String refusal;
            private List<String> annotations;
        }
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private PromptTokensDetails promptTokensDetails;
        private CompletionTokensDetails completionTokensDetails;

        @Data
        public static class PromptTokensDetails {
            private Integer cachedTokens;
            private Integer audioTokens;
        }

        @Data
        public static class CompletionTokensDetails {
            private Integer reasoningTokens;
            private Integer audioTokens;
            private Integer acceptedPredictionTokens;
            private Integer rejectedPredictionTokens;
        }
    }
}
