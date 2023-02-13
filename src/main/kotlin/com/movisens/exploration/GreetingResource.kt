package com.movisens.exploration

import com.oracle.svm.core.annotate.Inject
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.context.api.ManagedExecutorConfig
import io.smallrye.context.api.NamedInstance
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.core.eventbus.EventBus
import java.net.URI
import java.time.Clock
import java.util.UUID
import java.util.UUID.randomUUID
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.context.ThreadContext


@Path("/greeting")
@ApplicationScoped
class GreetingResource(val bus: EventBus) {

    val statusUrl = "/greeting/status/{id}"

    @POST
    @Path("/{name}")
    fun requestGreeting(@PathParam("name") name: String): Response {
        val id = randomUUID()
        println("Request greeting for $name")
        bus.send("greeting", Message(name, id))
        println("Send 202 response for $name")
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
            println(greeting)
            Response.ok().entity(greeting).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).entity("Greeting with id = $id was not found").build()
        }
    }
}

data class Message(val name: String, val id: UUID)

@ApplicationScoped
class GreetingService {

    @Inject
    @ManagedExecutorConfig(maxAsync = 2, maxQueued = 4, cleared = [ThreadContext.ALL_REMAINING])
    @NamedInstance("GreetingService")
    lateinit var configuredCustomExecutor: ManagedExecutor

    @ConsumeEvent(value= "greeting", blocking = false)
    fun generateGreeting(message: Message) {
        Uni.createFrom()
            .item(message)
            .emitOn(configuredCustomExecutor)
            .map { internalMessage ->
                val startTime = Clock.systemUTC().toString()
                println("Received Request to generate greeting for ${internalMessage.name}")
                Thread.sleep(2000)
                MemoryDatabase.greetings[internalMessage.id] = Greeting("Hi ${internalMessage.name} I greet you asynchronously!", startTime)
                println("Generated greeting for ${internalMessage.name}")
            }.subscribe()
    }
}

object MemoryDatabase {
    val greetings = mutableMapOf<UUID, Greeting>()
}

data class Greeting(val greeting: String, val startTime: String)