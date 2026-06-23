package xyz.nibblz.islandeconomist.features

interface Feature {
    val id: String
    val name: String

    fun init()
}