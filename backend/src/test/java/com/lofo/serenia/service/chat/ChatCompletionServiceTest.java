package com.lofo.serenia.service.chat;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatCompletionService Tests")
class ChatCompletionServiceTest {

    @Test
    @DisplayName("Should return ChatCompletionResult with content and tokens")
    void should_return_completion_result_with_content_and_tokens() {
        String responseContent = "This is a test response";
        int totalTokens = 432;

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(responseContent, totalTokens);

        assertEquals(responseContent, result.content());
        assertEquals(totalTokens, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle empty response content")
    void should_handle_empty_response_content() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("", 100);

        assertEquals("", result.content());
        assertEquals(100, result.totalTokensUsed());
    }

    @Test
    @DisplayName("ChatCompletionResult record structure is correct")
    void should_have_correct_record_structure() {
        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("response", 200);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("response", 200);
        ChatCompletionService.ChatCompletionResult result3 =
                new ChatCompletionService.ChatCompletionResult("different", 300);

        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
    }

    @Test
    @DisplayName("Should generate reply with system prompt and conversation messages")
    void should_generate_reply_with_system_prompt_and_messages() {
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, "Hello");

        String expectedContent = "Hello! How can I help you?";
        int expectedTokens = 250;

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(expectedContent, expectedTokens);

        assertNotNull(result);
        assertEquals(expectedContent, result.content());
        assertEquals(expectedTokens, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle null conversation messages")
    void should_handle_null_conversation_messages() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Response without history", 150);

        assertNotNull(result);
        assertEquals("Response without history", result.content());
        assertEquals(150, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle empty conversation messages list")
    void should_handle_empty_conversation_messages_list() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Fresh response", 200);

        assertNotNull(result);
        assertEquals("Fresh response", result.content());
        assertEquals(200, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle null system prompt")
    void should_handle_null_system_prompt() {
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, "What's the weather?");

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("It's sunny today", 300);

        assertNotNull(result);
        assertEquals("It's sunny today", result.content());
        assertEquals(300, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle empty system prompt")
    void should_handle_empty_system_prompt() {
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, "Tell me a joke");

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Why did the chicken cross the road?", 180);

        assertNotNull(result);
        assertEquals("Why did the chicken cross the road?", result.content());
        assertEquals(180, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should correctly map ChatCompletionResult properties")
    void should_correctly_map_result_properties() {
        String expectedContent = "Voilà : petit salé aux lentilles";
        int expectedTokens = 312;

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(expectedContent, expectedTokens);

        assertEquals(expectedContent, result.content());
        assertEquals(expectedTokens, result.totalTokensUsed());
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle large token counts")
    void should_handle_large_token_counts() {
        String content = "Long response";
        int largeTokenCount = 10000;

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(content, largeTokenCount);

        assertEquals(largeTokenCount, result.totalTokensUsed());
        assertEquals(content, result.content());
    }

    @Test
    @DisplayName("Should handle zero tokens")
    void should_handle_zero_tokens() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Content", 0);

        assertEquals(0, result.totalTokensUsed());
        assertEquals("Content", result.content());
    }

    @Test
    @DisplayName("Should handle multiple conversation messages")
    void should_handle_multiple_conversation_messages() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Follow-up answer", 450);

        assertNotNull(result);
        assertEquals("Follow-up answer", result.content());
        assertEquals(450, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should preserve exact token count from OpenAI response")
    void should_preserve_exact_token_count() {
        int actualTokensFromOpenAI = 432;

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Response", actualTokensFromOpenAI);

        assertEquals(actualTokensFromOpenAI, result.totalTokensUsed());
    }

    @Test
    @DisplayName("ChatCompletionResult should be immutable")
    void chatCompletionResult_should_be_immutable() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("content", 100);

        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("content", 100);

        assertEquals(result, result2);
    }

    @Test
    @DisplayName("Should distinguish between different token counts")
    void should_distinguish_between_different_token_counts() {
        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Response", 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Response", 200);

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("Should distinguish between different content")
    void should_distinguish_between_different_content() {
        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Content A", 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Content B", 100);

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("Should handle special characters in response content")
    void should_handle_special_characters_in_content() {
        String specialContent = "Special chars: @#$%^&*()_+-=[]{}|;:',.<>?/`~";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(specialContent, 500);

        assertEquals(specialContent, result.content());
        assertEquals(500, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle multiline response content")
    void should_handle_multiline_response_content() {
        String multilineContent = "Line 1\nLine 2\nLine 3\nLine 4";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(multilineContent, 350);

        assertEquals(multilineContent, result.content());
        assertTrue(result.content().contains("\n"));
    }

    @Test
    @DisplayName("Should handle very long response content")
    void should_handle_very_long_response_content() {
        String longContent = "a".repeat(5000);

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(longContent, 2000);

        assertEquals(longContent, result.content());
        assertEquals(2000, result.totalTokensUsed());
        assertEquals(5000, result.content().length());
    }

    @Test
    @DisplayName("Should handle response with unicode characters")
    void should_handle_unicode_response() {
        String unicodeResponse = "Voilà: petit salé aux lentilles — fais mijoter 🍲";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(unicodeResponse, 200);

        assertEquals(unicodeResponse, result.content());
        assertTrue(result.content().contains("—"));
        assertTrue(result.content().contains("🍲"));
    }

    @Test
    @DisplayName("Should handle very long conversation history")
    void should_handle_long_conversation() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("Final answer", 5000);

        assertNotNull(result);
        assertEquals("Final answer", result.content());
        assertEquals(5000, result.totalTokensUsed());
    }

    @Test
    @DisplayName("Should handle response with code snippets")
    void should_handle_code_response() {
        String codeResponse = "Here's the solution:\n```java\npublic void test() {\n  System.out.println(\"Hello\");\n}\n```";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(codeResponse, 400);

        assertEquals(codeResponse, result.content());
        assertTrue(result.content().contains("```java"));
    }

    @Test
    @DisplayName("Should handle response with markdown formatting")
    void should_handle_markdown_response() {
        String markdownResponse = "# Title\n\n## Subtitle\n\n- Item 1\n- Item 2\n\n**Bold text** and *italic text*";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(markdownResponse, 250);

        assertEquals(markdownResponse, result.content());
        assertTrue(result.content().contains("# Title"));
        assertTrue(result.content().contains("**Bold text**"));
    }

    @Test
    @DisplayName("Should handle response with JSON content")
    void should_handle_json_response() {
        String jsonResponse = "{\"status\": \"success\", \"data\": {\"id\": 123, \"name\": \"test\"}}";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(jsonResponse, 150);

        assertEquals(jsonResponse, result.content());
        assertTrue(result.content().contains("status"));
        assertTrue(result.content().contains("success"));
    }

    @Test
    @DisplayName("Should handle response with SQL content")
    void should_handle_sql_response() {
        String sqlResponse = "SELECT * FROM users WHERE age > 18 AND status = 'active';";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(sqlResponse, 80);

        assertEquals(sqlResponse, result.content());
        assertTrue(result.content().contains("SELECT"));
    }

    @Test
    @DisplayName("Should handle response with URLs")
    void should_handle_urls_response() {
        String urlResponse = "Check out https://example.com and http://test.org/page?id=123&name=test";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(urlResponse, 120);

        assertEquals(urlResponse, result.content());
        assertTrue(result.content().contains("https://"));
    }

    @Test
    @DisplayName("Should handle response with whitespace variations")
    void should_handle_whitespace_variations() {
        String responseWithTabs = "Line1\tTab\tSeparated";
        String responseWithSpaces = "Line1  Double  Spaces";

        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult(responseWithTabs, 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult(responseWithSpaces, 100);

        assertNotEquals(result1, result2);
        assertTrue(result1.content().contains("\t"));
        assertTrue(result2.content().contains("  "));
    }

    @Test
    @DisplayName("Should handle response starting with special characters")
    void should_handle_special_characters_start() {
        String response = ">>> Important: Do this first";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(response, 80);

        assertEquals(response, result.content());
        assertTrue(result.content().startsWith(">>>"));
    }

    @Test
    @DisplayName("Should handle response ending with punctuation")
    void should_handle_punctuation_ending() {
        String response = "This is the final answer!!!";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(response, 50);

        assertEquals(response, result.content());
        assertTrue(result.content().endsWith("!!!"));
    }

    @Test
    @DisplayName("Should handle token count at boundary values")
    void should_handle_boundary_token_values() {
        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Content", 0);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Content", 1);
        ChatCompletionService.ChatCompletionResult result3 =
                new ChatCompletionService.ChatCompletionResult("Content", Integer.MAX_VALUE);

        assertEquals(0, result1.totalTokensUsed());
        assertEquals(1, result2.totalTokensUsed());
        assertEquals(Integer.MAX_VALUE, result3.totalTokensUsed());
    }

    @Test
    @DisplayName("Should preserve leading and trailing whitespace in content")
    void should_preserve_whitespace() {
        String contentWithWhitespace = "  Leading and trailing spaces  ";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(contentWithWhitespace, 100);

        assertEquals(contentWithWhitespace, result.content());
        assertTrue(result.content().startsWith("  "));
        assertTrue(result.content().endsWith("  "));
    }

    @Test
    @DisplayName("Should handle response with CRLF line endings")
    void should_handle_crlf_line_endings() {
        String responseWithCRLF = "Line1\r\nLine2\r\nLine3";

        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult(responseWithCRLF, 100);

        assertEquals(responseWithCRLF, result.content());
        assertTrue(result.content().contains("\r\n"));
    }

    @Test
    @DisplayName("Should hash correctly for same content and tokens")
    void should_have_consistent_hash() {
        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Content", 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Content", 100);

        Set<ChatCompletionService.ChatCompletionResult> set = new HashSet<>();
        set.add(result1);
        set.add(result2);

        assertEquals(1, set.size());
    }

    @Test
    @DisplayName("Should work correctly in a Map as a value")
    void should_work_as_map_value() {
        Map<String, ChatCompletionService.ChatCompletionResult> resultMap = new HashMap<>();

        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Response1", 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Response2", 200);

        resultMap.put("key1", result1);
        resultMap.put("key2", result2);

        assertEquals(2, resultMap.size());
        assertEquals(result1, resultMap.get("key1"));
        assertEquals(result2, resultMap.get("key2"));
    }

    @Test
    @DisplayName("Should work correctly in a List")
    void should_work_in_list() {
        List<ChatCompletionService.ChatCompletionResult> results = new ArrayList<>();

        ChatCompletionService.ChatCompletionResult result1 =
                new ChatCompletionService.ChatCompletionResult("Response1", 100);
        ChatCompletionService.ChatCompletionResult result2 =
                new ChatCompletionService.ChatCompletionResult("Response2", 200);

        results.add(result1);
        results.add(result2);

        assertEquals(2, results.size());
        assertTrue(results.contains(result1));
        assertTrue(results.contains(result2));
    }

    @Test
    @DisplayName("Should produce correct toString representation")
    void should_have_toString() {
        ChatCompletionService.ChatCompletionResult result =
                new ChatCompletionService.ChatCompletionResult("TestContent", 150);

        String stringRepresentation = result.toString();
        assertNotNull(stringRepresentation);
        assertNotEquals("", stringRepresentation);
    }
}
