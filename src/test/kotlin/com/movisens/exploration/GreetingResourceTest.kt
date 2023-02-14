package com.movisens.exploration

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import java.time.Clock
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetingResourceTest {

    @Test
    fun reproduceTimeoutException() {
        val statusLocation = given()
          .`when`().post("/greeting")
          .then()
            .statusCode(202)
            .extract()
            .header("Location")

        Thread.sleep(33000)

        given()
            .`when`().get(statusLocation)
            .then()
            .statusCode(200)
            .body(`is`("Hi Simon I greet you asynchronously!"))
    }

    @Test
    fun multipleRequests(){
        val names = listOf("Simon", "Johannes", "Robert", "Jürgen", "Jörg","Tim")
        val locations = mutableListOf<String>()
        for (name in names){
            println("Send request for greeting: ${Clock.systemUTC().instant()}")
            given()
                .`when`().post("/greeting/$name")
                .then()
                .statusCode(202)
                .extract()
                .header("Location")
                .let { locations.add(it) }
        }

        Thread.sleep((locations.size / 2 * 2000).toLong())

        for (location in locations){
            given()
                .`when`().get(location)
                .then()
                .statusCode(200)
                .body(containsString("I greet you asynchronously!"))
                .extract()
                .body().`as`(Greeting::class.java)
                .let { println(it) }
        }
    }
}