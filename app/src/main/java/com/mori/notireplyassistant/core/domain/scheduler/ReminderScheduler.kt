package com.mori.notireplyassistant.core.domain.scheduler

interface ReminderScheduler {
    fun schedule(reminderId: Long, scheduledTimeMillis: Long)
    fun cancel(reminderId: Long)
    fun cancelAll()
}
