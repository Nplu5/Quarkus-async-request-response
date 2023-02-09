package com.movisens.exploration

import io.quarkus.vertx.ConsumeEvent
import io.vertx.mutiny.core.eventbus.EventBus
import java.net.URI
import java.util.UUID
import java.util.UUID.randomUUID
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

@Path("/greeting")
@ApplicationScoped
class GreetingResource(val bus: EventBus) {

    val statusUrl = "/greeting/status/{id}"

    @POST
    fun requestGreeting(): Response {
        val id = randomUUID()
        println("Request greeting")
        bus.requestAndForget<Message>("greeting", Message("Simon", id))
        println("Send 202 response")
        return Response
            .accepted()
            .header("Location", statusUrl.replace("{id}", id.toString()))
            .build()
    }

    @GET
    @Path("status/{id}")
    fun greetingStatus(@PathParam("id") id: UUID): Response {
        return if(MemoryDatabase.greetings.containsKey(id)){
            Response.seeOther(URI("/greeting/$id")).build()
        } else {
            Response.ok().entity("In progress").build()
        }
    }

    @GET
    @Path("/{id}")
    fun greeting(@PathParam("id") id: UUID): Response {
        return if (MemoryDatabase.greetings.containsKey(id)){
            val greeting = MemoryDatabase.greetings[id]
            MemoryDatabase.greetings.remove(id)
            Response.ok().entity(greeting).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).entity("Greeting with id = $id was not found").build()
        }
    }
}

data class Message(val name: String, val id: UUID)

@ApplicationScoped
class GreetingService {

    @ConsumeEvent(value= "greeting", blocking = true)
    fun generateGreeting(message: Message) {
        println("Received Request to generate greeting")
        Thread.sleep(32000)
        MemoryDatabase.greetings[message.id] = "Hi ${message.name} I greet you asynchronously!"
        println("Generated greeting")
    }
}

object MemoryDatabase {
    val greetings = mutableMapOf<UUID, String>()
}