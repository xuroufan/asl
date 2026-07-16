package com.hackfuture.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * 后端统一响应包装 — 对应 Java 后端 Result<T>
 *
 * HTTP body:
 * ```json
 * {"code":200,"msg":"success","data":{...}}
 * ```
 */
@Serializable
data class ApiResult(
    val code: Int = 200,
    val msg: String = "success",
    val data: JsonElement? = null,
)

/** 将 ApiResult.data 解析为具体类型 */
inline fun <reified T> ApiResult.parseData(json: Json): T? {
    return try {
        data?.let { json.decodeFromJsonElement(serializer<T>(), it) }
    } catch (e: Exception) {
        null
    }
}
