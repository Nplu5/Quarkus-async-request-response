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
    // TODO: Check why redirect immediately contains quarkus host
    // TODO: fix:
    /*
    023-02-01 17:07:19,614 ERROR [io.qua.mut.run.MutinyInfrastructure] (vert.x-eventloop-thread-2) Mutiny had to drop the following exception: (TIMEOUT,-1) Timed out after waiting 30000(ms) for a reply. address: __vertx.reply.3, repliedAddress: greeting
	at io.vertx.core.eventbus.impl.ReplyHandler.handle(ReplyHandler.java:76)
	at io.vertx.core.eventbus.impl.ReplyHandler.handle(ReplyHandler.java:24)
	at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:932)
	at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:903)
	at io.vertx.core.impl.EventLoopContext.emit(EventLoopContext.java:55)
	at io.vertx.core.impl.DuplicatedContext.emit(DuplicatedContext.java:158)
	at io.vertx.core.impl.ContextInternal.emit(ContextInternal.java:194)
	at io.vertx.core.impl.VertxImpl$InternalTimerHandler.run(VertxImpl.java:921)
	at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98)
	at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:153)
	at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:174)
	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:167)
	at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:470)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:569)
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.base/java.lang.Thread.run(Thread.java:829)
     */
    // TODO: Check if event bus can be limited in concurrent executions limit worker in thread pool that I can generate:
    // https://stackoverflow.com/questions/60760290/right-way-to-start-a-worker-thread-with-quarkus
    @ConsumeEvent(value= "greeting", blocking = true)
    fun generateGreeting(message: Message): String{
        println("Received Request to generate greeting")
        Thread.sleep(32000)
        MemoryDatabase.greetings[message.id] = "Hi ${message.name} I greet you asynchronously!"
        println("Generated greeting")
        return "Random"
    }
}

object MemoryDatabase {
    val greetings = mutableMapOf<UUID, String>()
}