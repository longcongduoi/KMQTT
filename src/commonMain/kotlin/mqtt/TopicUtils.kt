package mqtt

fun String.containsWildcard(): Boolean {
    return this.contains("#") || this.contains("+")
}

fun String.isValidTopic(): Boolean {
    if (this.isEmpty())
        return false

    if (this.contains("#")) {
        if (this.count { it.toString().contains("#") } > 1 || (this != "#" && !this.endsWith("/#")))
            return false
    }
    if (this.contains("+")) { // Taken from Paho MQTT Java
        for (i in 0 until length) {
            if (this[i] == '+') {
                val prev = if (i - 1 >= 0) this[i - 1] else null
                val next = if (i + 1 < length) this[i + 1] else null
                if (prev != '/' && prev != null || next != '/' && next != null)
                    return false
            }
        }
    }

    return true
}

// Taken from Paho MQTT Java
fun String.matchesWildcard(wildcardTopic: String): Boolean {
    var curn = 0
    var curf = 0
    val curnEnd: Int = this.length
    val curfEnd: Int = wildcardTopic.length

    if (this.containsWildcard())
        return false
    if (!this.isValidTopic() || !wildcardTopic.isValidTopic())
        return false

    // The Server MUST NOT match Topic Filters starting with a wildcard character (# or +) with Topic Names beginning with a $ character
    if ((wildcardTopic.startsWith("+") || wildcardTopic.startsWith("#")) && this.startsWith("$"))
        return false

    if (this == wildcardTopic) {
        return true
    }

    while (curf < curfEnd && curn < curnEnd) {
        if (this[curn] == '/' && wildcardTopic[curf] != '/')
            break
        if (wildcardTopic[curf] != '+' && wildcardTopic[curf] != '#' && wildcardTopic[curf] != this[curn])
            break
        if (wildcardTopic[curf] == '+') { // skip until we meet the next separator, or end of string
            var nextpos = curn + 1
            while (nextpos < curnEnd && this[nextpos] != '/')
                nextpos = ++curn + 1
        } else if (wildcardTopic[curf] == '#')
            curn = curnEnd - 1 // skip until end of string
        curf++
        curn++
    }

    return curn == curnEnd && curf == curfEnd
}

fun String.isSharedTopicFilter(): Boolean {
    val split = this.split("/")
    if (split.size < 3)
        return false
    if (split[0] == "\$share" && split[1].isNotEmpty() && !split[1].contains("+") && !split[1].contains("#") && this.substringAfter(
            split[1] + "/"
        ).isValidTopic()
    )
        return true
    return false
}

fun String.getSharedTopicFilter(): String? {
    if (isSharedTopicFilter()) {
        val split = this.split("/")
        return this.substringAfter(split[1] + "/")
    }
    return null
}

fun String.getSharedTopicShareName(): String? {
    if (isSharedTopicFilter()) {
        val split = this.split("/")
        return split[1]
    }
    return null
}