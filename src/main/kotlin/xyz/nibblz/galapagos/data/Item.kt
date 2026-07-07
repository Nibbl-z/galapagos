package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable

enum class ItemLocation {
    INFINIBAG,
    INFINIVAULT
}

@Serializable
data class Item(
    val name: String,
    var count: Int,
    val isCosmeticToken: Boolean
) : Cloneable {
    public override fun clone(): Item {
        return super.clone() as Item
    }
}