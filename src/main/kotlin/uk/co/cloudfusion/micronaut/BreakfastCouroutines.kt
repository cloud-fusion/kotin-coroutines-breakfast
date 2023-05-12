package uk.co.cloudfusion.micronaut

import io.micronaut.context.annotation.Bean
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

@Controller("/api/v1/coroutines")
class AiChiefCoroutinesController(
    val fridgeCoroutineClient: FridgeCoroutineClient,
    val hobCoroutineClient: HobCoroutineClient,
    val childrenCoroutineClient: ChildrenCoroutineClient,
) {

    @Get("/breakfaslt/seq")
    suspend fun getBreakfastSeq(): List<FoodItems> {
        // This is a sequence of calls, each one waiting for the previous one to complete
        val sausages = fridgeCoroutineClient.getSassages()
        val bacon = fridgeCoroutineClient.getBacon()
        val eggs = fridgeCoroutineClient.getEgg()

        val friedItems = hobCoroutineClient.fry(sausages, bacon, eggs)
        childrenCoroutineClient.call()
        return friedItems.toList()
    }

    @Get("breakfast/async")
    suspend fun getBreakfastAsync(): List<FoodItems> = coroutineScope {
        val sausages = async { fridgeCoroutineClient.getSassages() }
        val bacon = async { fridgeCoroutineClient.getBacon() }
        val eggs = async { fridgeCoroutineClient.getEgg() }

        val friedItems = hobCoroutineClient.fry(awaitAll(sausages, bacon, eggs))
        childrenCoroutineClient.call()
        friedItems.toList()
    }
}

@Bean
class FridgeCoroutineClient {

    private suspend fun delayAndLog(FoodItems: FoodItems): FoodItems = coroutineScope {
        delay(1000)
        println("Got $FoodItems")
        FoodItems
    }

    suspend fun getSassages() = delayAndLog(FoodItems.Sausages("Cumberland"))

    suspend fun getBacon() = delayAndLog(FoodItems.Bacon(1))

    suspend fun getEgg() = delayAndLog(FoodItems.Egg(1))
}

@Bean
class ChildrenCoroutineClient {

    fun call(): Flow<Void> {
        return flow { }
    }
}

@Bean
class HobCoroutineClient {


    fun fry(vararg foodItems: FoodItems): Flow<FoodItems> {
        return foodItems.asFlow()
    }

    fun fry(foodItems: List<FoodItems>): Flow<FoodItems> {
        return foodItems.asFlow()
    }
}
