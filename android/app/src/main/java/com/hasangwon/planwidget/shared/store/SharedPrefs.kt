package com.hasangwon.planwidget.shared.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.hasangwon.planwidget.shared.model.RawValued

/** 설정 저장소 추상화 — 앱은 SharedPreferences, JVM 테스트는 메모리 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun getLong(key: String): Long?
    fun putLong(key: String, value: Long)
    fun remove(key: String)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = map[key] as? String
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = map[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getInt(key: String): Int? = map[key] as? Int
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getLong(key: String): Long? = map[key] as? Long
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

class PreferencesKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    override fun getInt(key: String): Int? = if (prefs.contains(key)) prefs.getInt(key, 0) else null
    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
    override fun getLong(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    override fun putLong(key: String, value: Long) = prefs.edit { putLong(key, value) }
    override fun remove(key: String) = prefs.edit { remove(key) }
}

/**
 * 앱·위젯이 함께 쓰는 설정 저장소 (iOS SharedDefaults 대응).
 * 안드로이드는 앱과 위젯이 같은 프로세스라 App Group 이 필요 없다 — SharedPreferences 하나를 Application 에서 1회 연결한다.
 * 값은 캐시하지 않으므로 설정 변경은 즉시 반영된다. 키 이름은 iOS 와 같게 유지한다.
 */
object SharedPrefs {
    private const val FILE_NAME = "planwidget"

    @Volatile
    var store: KeyValueStore = InMemoryKeyValueStore()

    fun init(context: Context) {
        store = PreferencesKeyValueStore(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))
    }

    /** 문자열 원시값으로 저장한 enum 읽기 — 없거나 알 수 없는 값이면 기본값 */
    inline fun <reified T> value(key: String, default: T): T where T : Enum<T>, T : RawValued {
        val raw = store.getString(key) ?: return default
        return enumValues<T>().firstOrNull { it.raw == raw } ?: default
    }

    fun set(key: String, value: RawValued) = store.putString(key, value.raw)
}
