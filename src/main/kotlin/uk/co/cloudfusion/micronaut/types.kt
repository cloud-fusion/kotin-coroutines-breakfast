package uk.co.cloudfusion.micronaut

import io.micronaut.serde.annotation.Serdeable


@Serdeable
sealed class FoodItems() {
    @Serdeable
    data class Sausages(val type: String) : FoodItems()

    @Serdeable
    data class Bacon(val thinknees: Int) : FoodItems()

    @Serdeable
    data class Egg(val size: Int) : FoodItems()
}
