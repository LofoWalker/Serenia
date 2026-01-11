package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.out.admin.DashboardDTO;
import com.lofo.serenia.rest.dto.out.admin.TimelineDTO;
import com.lofo.serenia.rest.dto.out.admin.UserDetailDTO;
import com.lofo.serenia.rest.dto.out.admin.UserListDTO;
import com.lofo.serenia.service.admin.AdminStatsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminResource {

    private final AdminStatsService adminStatsService;

    public AdminResource(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GET
    @Path("/dashboard")
    public Response getDashboard() {
        DashboardDTO dashboard = adminStatsService.getDashboard();
        return Response.ok(dashboard).build();
    }

    @GET
    @Path("/timeline")
    public Response getTimeline(
            @QueryParam("metric") @DefaultValue("messages") String metric,
            @QueryParam("days") @DefaultValue("7") int days
    ) {
        if (days != 7 && days != 30) {
            days = 7;
        }
        TimelineDTO timeline = adminStatsService.getTimeline(metric, days);
        return Response.ok(timeline).build();
    }

    @GET
    @Path("/users")
    public Response getUsers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        UserListDTO users = adminStatsService.getUserList(page, size);
        return Response.ok(users).build();
    }

    @GET
    @Path("/users/{email}")
    public Response getUserByEmail(@PathParam("email") String email) {
        UserDetailDTO user = adminStatsService.getUserByEmail(email);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(user).build();
    }
}

