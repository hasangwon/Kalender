package com.hasangwon.planwidget.shared.store

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.hasangwon.planwidget.shared.model.AnniversaryEntry
import com.hasangwon.planwidget.shared.model.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule ORDER BY createdAt")
    fun observeAll(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedule ORDER BY createdAt")
    suspend fun getAll(): List<Schedule>

    /** 표시 기간에 나올 수 있는 일정만 — 단일 일정은 기간 안의 것만, 반복 일정은 판정에 맡긴다 */
    @Query(
        "SELECT * FROM schedule WHERE recurrenceRaw != 'none' " +
            "OR (startDate >= :rangeStartMillis AND startDate < :rangeEndMillis)",
    )
    suspend fun getCandidates(rangeStartMillis: Long, rangeEndMillis: Long): List<Schedule>

    /** 알림을 켠 일정 중 기간에 나올 수 있는 것 */
    @Query(
        "SELECT * FROM schedule WHERE notifies = 1 AND (recurrenceRaw != 'none' " +
            "OR (startDate >= :rangeStartMillis AND startDate < :rangeEndMillis))",
    )
    suspend fun getNotifyingCandidates(rangeStartMillis: Long, rangeEndMillis: Long): List<Schedule>

    @Query("SELECT id FROM schedule")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM schedule")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(schedule: Schedule)

    @Upsert
    suspend fun upsertAll(schedules: List<Schedule>)

    @Delete
    suspend fun delete(schedule: Schedule)
}

@Dao
interface AnniversaryDao {
    @Query("SELECT * FROM anniversary ORDER BY createdAt")
    fun observeAll(): Flow<List<AnniversaryEntry>>

    @Query("SELECT * FROM anniversary ORDER BY createdAt")
    suspend fun getAll(): List<AnniversaryEntry>

    @Query("SELECT id FROM anniversary")
    suspend fun getAllIds(): List<String>

    @Upsert
    suspend fun upsert(entry: AnniversaryEntry)

    @Upsert
    suspend fun upsertAll(entries: List<AnniversaryEntry>)

    @Delete
    suspend fun delete(entry: AnniversaryEntry)
}

/**
 * 앱/위젯 공용 Room 데이터베이스 (iOS ScheduleStore.makeContainer 대응).
 * 같은 프로세스라 공유 컨테이너가 필요 없다. 인스턴스는 Application 이 하나만 들고 있다.
 */
@Database(entities = [Schedule::class, AnniversaryEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun anniversaryDao(): AnniversaryDao

    companion object {
        const val NAME = "planwidget.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
