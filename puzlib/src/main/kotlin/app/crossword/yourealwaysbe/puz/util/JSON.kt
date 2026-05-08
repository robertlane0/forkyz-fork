/**
 * org.json style interface to preferred JSON library
 */

package app.crossword.yourealwaysbe.puz.util

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

import li.songe.json5.Json5

class JSONException(cause : Throwable) : Exception(cause)

@Throws(JSONException::class)
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun parseJSONObject(s : String) : JSONObject {
    try {
        val e = json.parseToJsonElement(s)
        return JSONObject(e.jsonObject)
    } catch (err : IllegalArgumentException) {
        throw JSONException(err)
    }
}

@Throws(IOException::class, JSONException::class)
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun parseJSONObject(input : Source) : JSONObject {
    try {
        val e : JsonElement = json.decodeFromSource(input)
        return JSONObject(e.jsonObject)
    } catch (err : IllegalArgumentException) {
        throw JSONException(err)
    }
}

@Throws(IOException::class)
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun writeJSONObject(obj : JSONObject, output : Sink) {
    try {
        json.encodeToSink(obj.obj, output)
    } catch (err : SerializationException) {
        throw IOException(err)
    }
}

@Throws(IOException::class, JSONException::class)
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun parseJSONArray(input : Source) : JSONArray {
    try {
        val e : JsonElement = json.decodeFromSource(input)
        return JSONArray(e.jsonArray)
    } catch (err : IllegalArgumentException) {
        throw JSONException(err)
    }
}

@Throws(JSONException::class)
fun parseJSON5Object(data : String) : JSONObject {
    try {
        return JSONObject(Json5.parseToJson5Element(data).jsonObject)
    } catch (err : IllegalArgumentException) {
        throw JSONException(err)
    }
}

val JsonObject.asForkyzJSONObject : JSONObject
    get() { return JSONObject(this) }

class JSONObject internal constructor(internal val obj : JsonObject) {
    /**
     * As Kotlin serialization JSON
     */
    val toKotlinJSON : JsonObject
        get() { return obj }

    /**
     * The keys in this object
     */
    val keys : Set<String> = obj.keys

    /**
     * True if object has the given key
     */
    fun has(key : String) : Boolean {
        return obj.containsKey(key)
    }

    /**
     * Get Bool with key or throw exception
     *
     * @throws JSONException
     */
    fun getBoolean(key : String) : Boolean {
        return tryJson { obj[key]!!.jsonPrimitive.boolean}
    }

    /**
     * Get int with key or return null
     */
    fun optBoolean(key : String) : Boolean? {
        return tryJsonNull { obj[key]?.jsonPrimitive?.booleanOrNull }
    }

    /**
     * Get int with key or return default
     */
    fun optBoolean(key : String, default : Boolean) : Boolean {
        return optBoolean(key) ?: default
    }

    /**
     * Get double with key or throw exception
     *
     * @throws JSONException
     */
    fun getDouble(key : String) : Double {
        return tryJson { obj[key]!!.jsonPrimitive.double }
    }

    /**
     * Get int with key or throw exception
     *
     * @throws JSONException
     */
    fun getInt(key : String) : Int {
        return tryJson { obj[key]!!.jsonPrimitive.int }
    }

    /**
     * Get int with key or return null
     */
    fun optInt(key : String) : Int? {
        return tryJsonNull { obj[key]?.jsonPrimitive?.intOrNull }
    }

    /**
     * Get int with key or return default
     */
    fun optInt(key : String, default : Int) : Int {
        return optInt(key) ?: default
    }

    /**
     * Get object with key or throw exception
     *
     * @throws JSONException
     */
    fun getJSONObject(key : String) : JSONObject {
        return tryJson { JSONObject(obj[key]!!.jsonObject) }
    }

    /**
     * Get object with key or return null
     */
    fun optJSONObject(key : String) : JSONObject? {
        return tryJsonNull { obj[key]?.jsonObject?.let { JSONObject(it) } }
    }

    /**
     * Get object with key or throw exception
     *
     * @throws JSONException
     */
    fun getJSONArray(key : String) : JSONArray {
        return tryJson { JSONArray(obj[key]!!.jsonArray) }
    }

    /**
     * Get object with key or return null
     */
    fun optJSONArray(key : String) : JSONArray? {
        return tryJsonNull { obj[key]?.jsonArray?.let { JSONArray(it) } }
    }

    /**
     * Get long with key or throw exception
     *
     * @throws JSONException
     */
    fun getLong(key : String) : Long {
        return tryJson { obj[key]!!.jsonPrimitive.long }
    }

    /**
     * Get long with key or null
     */
    fun optLong(key : String) : Long? {
        return tryJsonNull { obj[key]?.jsonPrimitive?.longOrNull }
    }

    /**
     * Get string with key or throw exception
     *
     * @throws JSONException
     */
    fun getString(key : String) : String {
        return tryJson { obj[key]!!.jsonPrimitive.contentOrNull!! }
    }

    /**
     * Get string with key or null
     */
    fun optString(key : String) : String? {
        return tryJsonNull { obj[key]?.jsonPrimitive?.contentOrNull }
    }

    /**
     * Get string with key or default value
     */
    fun optString(key : String, default : String) : String {
        return optString(key) ?: default
    }

    /**
     * Get string with key or return null if null or empty
     */
    fun optStringEmptyNull(key : String) : String? {
        return tryJsonNull {
            val s = obj[key]?.jsonPrimitive?.contentOrNull
            if (s?.isEmpty() ?: true) null else s
        }
    }

    /**
     * Get field of any type
     *
     * Tries all possibilities or fails with exception
     */
    fun get(key : String) : Any {
        return opt(key)
            ?: throw JSONException(
                IllegalArgumentException(
                    "Key $key not of a recognised type",
                ),
            )
    }

    /**
     * Get field of any type
     *
     * Tries all possibilities or null
     */
    fun opt(key : String) : Any? {
        if (!has(key))
            return null

        return optBoolean(key)
            ?: optInt(key)
            ?: optJSONArray(key)
            ?: optJSONObject(key)
            ?: optLong(key)
            ?: optString(key)
    }

    override fun toString() : String {
        return obj.toString()
    }
}

class JSONArray internal constructor(internal val arr : JsonArray) {
    val size : Int = arr.size

    /**
     * Get bool with key or throw exception
     *
     * @throws JSONException
     */
    fun getBoolean(i : Int) : Boolean {
        return tryJson { arr[i].jsonPrimitive.boolean }
    }

    /**
     * Get bool with key or return null
     */
    fun optBoolean(i : Int) : Boolean? {
        return tryJsonNull { arr[i].jsonPrimitive.booleanOrNull }
    }

    /**
     * Get int with key or throw exception
     *
     * @throws JSONException
     */
    fun getInt(i : Int) : Int {
        return tryJson { arr[i].jsonPrimitive.int }
    }

    /**
     * Get int with key or return null
     */
    fun optInt(i : Int) : Int? {
        return tryJsonNull { arr[i].jsonPrimitive.intOrNull }
    }

    /**
     * Get int with key or return default
     */
    fun optInt(i : Int, default : Int) : Int {
        return optInt(i) ?: default
    }

    /**
     * Get object with key or throw exception
     *
     * @throws JSONException
     */
    fun getJSONObject(i : Int) : JSONObject {
        return tryJson { JSONObject(arr[i].jsonObject) }
    }

    /**
     * Get object with key or return null
     */
    fun optJSONObject(i : Int) : JSONObject? {
        return tryJsonNull { JSONObject(arr[i].jsonObject) }
    }

    /**
     * Get object with key or throw exception
     *
     * @throws JSONException
     */
    fun getJSONArray(i : Int) : JSONArray {
        return tryJson { JSONArray(arr[i].jsonArray) }
    }

    /**
     * Get object with key or return null
     */
    fun optJSONArray(i : Int) : JSONArray? {
        return tryJsonNull { JSONArray(arr[i].jsonArray) }
    }

    /**
     * Get long with key or throw exception
     *
     * @throws JSONException
     */
    fun getLong(i : Int) : Long {
        return tryJson { arr[i].jsonPrimitive.long }
    }

    /**
     * Get int with key or return null
     */
    fun optLong(i : Int) : Long? {
        return tryJsonNull { arr[i].jsonPrimitive.longOrNull }
    }

    /**
     * Get string with key or throw exception
     *
     * @throws JSONException
     */
    fun getString(i : Int) : String {
        return tryJson { arr[i].jsonPrimitive.contentOrNull!! }
    }

    /**
     * Get string with key or return null
     */
    fun optString(i : Int) : String? {
        return tryJsonNull { arr[i].jsonPrimitive.contentOrNull }
    }

    /**
     * Get field of any type
     *
     * Tries all possibilities or fails with exception
     */
    fun get(i : Int) : Any {
        return opt(i)
            ?: throw JSONException(
                IllegalArgumentException(
                    "Pos $i not of a recognised type",
                ),
            )
    }

    /**
     * Get field of any type
     *
     * Tries all possibilities or null
     */
    fun opt(i : Int) : Any? {
        return optBoolean(i)
            ?: optInt(i)
            ?: optJSONArray(i)
            ?: optJSONObject(i)
            ?: optLong(i)
            ?: optString(i)
    }

    override fun toString() : String {
        return arr.toString()
    }
}

/**
 * Scope for building JSON objects
 *
 * Offers put methods, use with buildJSONObject
 */
class JSONObjectBuilder internal constructor(
    private val builder : JsonObjectBuilder,
) {
    /**
     * Put a string, does nothing if value is null
     */
    fun put(key : String, value : String?) {
        value?.let { builder.put(key, it) }
    }

    /**
     * Put a number, does nothing if value is null
     */
    fun put(key : String, value : Number?) {
        value?.let { builder.put(key, it) }
    }

    /**
     * Put a Boolean, does nothing if value is null
     */
    fun put(key : String, value : Boolean?) {
        value?.let { builder.put(key, it) }
    }

    /**
     * Put a JSONObject, does nothing if value is null
     */
    fun put(key : String, value : JSONObject?) {
        value?.let { builder.put(key, it.obj) }
    }

    /**
     * Put a JSONArray, does nothing if value is null
     */
    fun put(key : String, value : JSONArray?) {
        value?.let { builder.put(key, it.arr) }
    }
}

/**
 * Build a JSON object using builder scope
 */
fun buildJSONObject(builderAction : JSONObjectBuilder.() -> Unit) : JSONObject {
    return JSONObject(
        buildJsonObject {
            JSONObjectBuilder(this).apply(builderAction)
        },
    )
}

/**
 * Scope for building JSON arrays
 *
 * Offers put methods, use with buildJSONArray
 */
class JSONArrayBuilder internal constructor(
    private val builder : JsonArrayBuilder,
) {
    /**
     * Add a string, does nothing if value is null
     */
    fun add(value : String?) {
        value?.let { builder.add(it) }
    }

    /**
     * Add a number, does nothing if value is null
     */
    fun add(value : Number?) {
        value?.let { builder.add(it) }
    }

    /**
     * Add a Boolean, does nothing if value is null
     */
    fun add(value : Boolean?) {
        value?.let { builder.add(it) }
    }

    /**
     * Add a JSONObject, does nothing if value is null
     */
    fun add(value : JSONObject?) {
        value?.let { builder.add(it.obj) }
    }

    /**
     * Add a JSONArray, does nothing if value is null
     */
    fun add(value : JSONArray?) {
        value?.let { builder.add(it.arr) }
    }

    /**
     * Add JSON null
     */
    fun addNull() {
        builder.add(JsonNull)
    }
}

/**
 * Build a JSON object using builder scope
 */
fun buildJSONArray(builderAction : JSONArrayBuilder.() -> Unit) : JSONArray {
    return JSONArray(
        buildJsonArray {
            JSONArrayBuilder(this).apply(builderAction)
        },
    )
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private val json = Json {
    allowComments = true
    allowTrailingComma = true
    isLenient = true
}

private fun <T> tryJson(run : () -> T) : T {
    try {
        return run()
    } catch (err : Exception) {
        throw JSONException(err)
    }
}

private fun <T> tryJsonNull(run : () -> T?) : T? {
    try {
        return run()
    } catch (err : Exception) {
        return null
    }
}

