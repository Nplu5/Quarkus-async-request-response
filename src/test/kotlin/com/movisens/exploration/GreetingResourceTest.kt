package com.movisens.exploration

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
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

        val resultLocation = given()
            .`when`().get("$statusLocation")
            .then()
            .statusCode(200)
            .body(`is`("Hi Simon I greet you asynchronously!"))
    }

}