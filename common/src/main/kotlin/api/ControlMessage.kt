package dev.voroscsoki.stratopolis.common.api

interface ControlMessage<T> {
    val payload: T
}