package com.lofo.serenia.resource;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.dto.in.MessageRequestDTO;
import com.lofo.serenia.service.chat.ConversationService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Authenticated
@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Conversations", description = "Manage chat conversations and messages")
public class ConversationResource {

    private final ConversationService conversationService;
    private final SecurityIdentity securityIdentity;
    private final JsonWebToken jwt;

    public ConversationResource(ConversationService conversationService, SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.conversationService = conversationService;
        this.securityIdentity = securityIdentity;
        this.jwt = jwt;
    }

    /**
     * Add a user message to the active conversation. If no conversation exists, create one.
     * Returns the assistant's reply.
     */
    @POST
    @Path("/add-message")
    @Operation(summary = "Send a user message", description = "Appends a user message to the active conversation and returns the assistant reply.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = MessageRequestDTO.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Assistant reply returned", content = @Content(schema = @Schema(implementation = ChatMessage.class))),
            @APIResponse(responseCode = "400", description = "Missing or blank content"),
            @APIResponse(responseCode = "401", description = "User not authenticated")
    })
    public Response addMessage(MessageRequestDTO request) {
        UUID userId = getAuthenticatedUserId();
        if (request.content() == null || request.content().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("content must be provided").build();
        }

        ChatMessage assistantMessage = conversationService.addUserMessage(userId, request.content());
        return Response.ok(assistantMessage).build();
    }

    /**
     * Optional: fetch all decrypted messages for the conversation
     */
    @GET
    @Path("/{conversationId}/messages")
    @Operation(summary = "List conversation messages", description = "Returns decrypted messages for the requested conversation after enforcing ownership.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Messages returned", content = @Content(schema = @Schema(implementation = ChatMessage.class))),
            @APIResponse(responseCode = "401", description = "User not authenticated"),
            @APIResponse(responseCode = "403", description = "Conversation does not belong to the user")
    })
    public Response getConversationMessages(@PathParam("conversationId") @Parameter(description = "Conversation identifier", required = true) UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        List<ChatMessage> messages = conversationService.getConversationMessages(conversationId, userId);
        return Response.ok(messages).build();
    }

    private UUID getAuthenticatedUserId() {
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return UUID.fromString(jwt.getSubject());
        }

        String principalName = securityIdentity.getPrincipal().getName();
        return UUID.fromString(principalName);
    }
}
