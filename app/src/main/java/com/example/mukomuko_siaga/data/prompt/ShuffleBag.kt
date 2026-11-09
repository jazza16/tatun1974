package com.example.mukomuko_siaga.data.prompt

class ShuffleBag<T>(private val items: List<T>) {
    init {
        require(items.isNotEmpty()) { "ShuffleBag tidak boleh kosong" }
    }

    private val bag = ArrayDeque<T>()

    fun next(): T {
        if (bag.isEmpty()) bag.addAll(items.shuffled())
        return bag.removeFirst()
    }
}
