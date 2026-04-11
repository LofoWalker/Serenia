package com.lofo.serenia.rest.resource;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.rest.dto.in.CreateConversationRequestDTO;
import com.lofo.serenia.rest.dto.in.MessageRequestDTO;
import com.lofo.serenia.rest.dto.in.RenameConversationRequestDTO;
import com.lofo.serenia.rest.dto.out.ConversationMessagesResponseDTO;
import com.lofo.serenia.rest.dto.out.ConversationSummaryDTO;
import com.lofo.serenia.rest.dto.out.MessageResponseDTO;
import com.lofo.serenia.service.chat.ChatOrchestrator;
import com.lofo.serenia.service.chat.ConversationService;
import com.lofo.serenia.service.chat.ProcessedMessageResult;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Authenticated
@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Conversations", description = "Manage chat conversations and messages")
public class ConversationResource {

    private final ConversationService conversationService;
    private final ChatOrchestrator chatOrchestrator;
    private final SecurityIdentity securityIdentity;
    private final JsonWebToken jwt;

    public ConversationResource(ConversationService conversationService, ChatOrchestrator chatOrchestrator,
                                SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.conversationService = conversationService;
        this.chatOrchestrator = chatOrchestrator;
        this.securityIdentity = securityIdentity;
        this.jwt = jwt;
    }

    @GET
    @Operation(summary = "List user conversations",
        description = "Returns all conversations for the authenticated user, ordered by last activity.")
    @APIResponse(responseCode = "200", description = "List of conversations returned")
    public Response listConversations() {
        UUID userId = getAuthenticatedUserId();
        List<ConversationSummaryDTO> conversations = conversationService.listUserConversations(userId);
        return Response.ok(conversations).build();
    }

    @POST
    @Operation(summary = "Create a new conversation")
    @RequestBody(content = @Content(schema = @Schema(implementation = CreateConversationRequestDTO.class)))
    @APIResponse(responseCode = "201", description = "Conversation created",
        content = @Content(schema = @Schema(implementation = ConversationSummaryDTO.class)))
    public Response createConversation(CreateConversationRequestDTO request) {
        UUID userId = getAuthenticatedUserId();
        String name = request != null ? request.name() : null;
        Conversation conversation = conversationService.createNewConversation(userId, name);
        ConversationSummaryDTO dto = new ConversationSummaryDTO(
            conversation.getId(), conversation.getName(), conversation.getLastActivityAt());
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @PUT
    @Path("/{id}/name")
    @Operation(summary = "Rename a conversation")
    @RequestBody(content = @Content(schema = @Schema(implementation = RenameConversationRequestDTO.class)))
    @APIResponse(responseCode = "200", description = "Conversation renamed")
    @APIResponse(responseCode = "403", description = "Conversation does not belong to user")
    public Response renameConversation(@PathParam("id") UUID id, RenameConversationRequestDTO request) {
        UUID userId = getAuthenticatedUserId();
        if (request.name() == null || request.name().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("name must be provided").build();
        }
        Conversation conversation = conversationService.renameConversation(id, userId, request.name());
        ConversationSummaryDTO dto = new ConversationSummaryDTO(
            conversation.getId(), conversation.getName(), conversation.getLastActivityAt());
        return Response.ok(dto).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a single conversation")
    @APIResponse(responseCode = "204", description = "Conversation deleted")
    @APIResponse(responseCode = "403", description = "Conversation does not belong to user")
    public Response deleteConversation(@PathParam("id") UUID id) {
        UUID userId = getAuthenticatedUserId();
        conversationService.deleteSingleConversation(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/messages")
    @Operation(summary = "Get messages for a specific conversation")
    @APIResponse(responseCode = "200", description = "Messages returned",
        content = @Content(schema = @Schema(implementation = ConversationMessagesResponseDTO.class)))
    @APIResponse(responseCode = "403", description = "Conversation does not belong to user")
    public Response getConversationMessages(@PathParam("id") UUID id) {
        UUID userId = getAuthenticatedUserId();
        List<ChatMessage> messages = conversationService.getConversationMessages(id, userId);
        ConversationMessagesResponseDTO response = new ConversationMessagesResponseDTO(id, messages);
        return Response.ok(response).build();
    }

    @POST
    @Path("/add-message")
    @Operation(summary = "Send a user message",
        description = "Appends a user message to a conversation and returns the assistant reply.")
    @RequestBody(content = @Content(schema = @Schema(implementation = MessageRequestDTO.class)))
    @APIResponse(responseCode = "200", description = "Assistant reply returned",
        content = @Content(schema = @Schema(implementation = MessageResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Missing or blank content")
    public Response addMessage(MessageRequestDTO request) {
        UUID userId = getAuthenticatedUserId();
        if (request.content() == null || request.content().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("content must be provided").build();
        }

        ProcessedMessageResult result = chatOrchestrator.processUserMessage(
            userId, request.content(), request.conversationId());
        MessageResponseDTO response = MessageResponseDTO.from(result.conversationId(), result.assistantMessage());
        return Response.ok(response).build();
    }

    @GET
    @Path("/my-messages")
    @Operation(summary = "Get current user messages",
        description = "Returns messages for the most recent conversation.")
    @APIResponse(responseCode = "200", description = "Conversation with messages returned")
    @APIResponse(responseCode = "204", description = "No active conversation found")
    public Response getUserMessages() {
        UUID userId = getAuthenticatedUserId();
        Conversation conversation = conversationService.getActiveConversationByUserId(userId);

        if (conversation == null) {
            return Response.noContent().build();
        }

        List<ChatMessage> messages = conversationService.getConversationMessages(conversation.getId(), userId);
        ConversationMessagesResponseDTO response = new ConversationMessagesResponseDTO(conversation.getId(), messages);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/my-conversations")
    @Operation(summary = "Delete all user conversations")
    @APIResponse(responseCode = "204", description = "Conversations deleted")
    public Response deleteUserConversations() {
        UUID userId = getAuthenticatedUserId();
        conversationService.deleteUserConversations(userId);
        return Response.noContent().build();
    }

    private UUID getAuthenticatedUserId() {
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return UUID.fromString(jwt.getSubject());
        }

        String principalName = securityIdentity.getPrincipal().getName();
        return UUID.fromString(principalName);
    }
}

