package kuad

fun <T> List<T>.toNel() = NonEmptyList(this)

inline class NonEmptyList<T>(val list: List<T>) {
    init {
        require(list.isNotEmpty())
    }
}
