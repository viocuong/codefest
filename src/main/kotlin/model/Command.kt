package model

enum class Command(val value: String) {
    LEFT("1"),
    RIGHT("2"),
    UP("3"),
    DOWN("4"),
    BOMB("b"),
    STOP("x")
}

fun List<Command>.toDirection() = Direction(
    direction = map(Command::value).joinToString(separator = "")
)