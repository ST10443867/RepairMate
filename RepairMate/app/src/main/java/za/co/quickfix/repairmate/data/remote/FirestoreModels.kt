package za.co.quickfix.repairmate.data.remote

/*
 * Data classes for the Cloud Firestore REST API (https://firebase.google.com/docs/firestore/use-rest-api).
 * Firestore wraps every value in a typed object, e.g. {"stringValue": "Dell"} or {"integerValue": "42"}.
 * Null fields are skipped by Gson, so only the populated type is sent.
 */

data class FsValue(
    val stringValue: String? = null,
    /** Firestore sends 64-bit integers as strings in JSON. */
    val integerValue: String? = null
)

data class FsDocument(
    /** Full resource path, e.g. projects/x/databases/(default)/documents/bookings/abc123 */
    val name: String? = null,
    val fields: Map<String, FsValue>? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

/** One row of a :runQuery response. When nothing matches, Firestore returns a row with no document. */
data class FsQueryResult(
    val document: FsDocument? = null,
    val readTime: String? = null
)

data class RunQueryRequest(val structuredQuery: StructuredQuery)
data class StructuredQuery(val from: List<CollectionSelector>, val where: QueryFilter? = null)
data class CollectionSelector(val collectionId: String)
data class QueryFilter(val fieldFilter: FieldFilter)
data class FieldFilter(val field: FieldReference, val op: String, val value: FsValue)
data class FieldReference(val fieldPath: String)
