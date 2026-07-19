package com.example.data.model

data class BulletinDay(
    val dayName: String = "",
    val serviceName: String = "",
    val time: String = "",
    val ushers: String = "",
    val worshipTeam: String = "",
    val decom: String = "",
    val sound: String = "",
    val notesUniform: String = ""
)

data class WeeklyBulletin(
    val id: String = "",
    val dateRange: String = "",
    val committee: String = "",
    val days: List<BulletinDay> = emptyList(),
    val generalAnnouncements: String = ""
) {
    companion object {
        fun default(): WeeklyBulletin {
            val weekDays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            return WeeklyBulletin(
                id = java.util.UUID.randomUUID().toString(),
                dateRange = "",
                committee = "",
                days = weekDays.map { BulletinDay(dayName = it) },
                generalAnnouncements = ""
            )
        }
    }
}

data class AnnualScheduleItem(
    val id: String = "",
    val date: String = "",
    val committee: String = "",
    val description: String = "",
    val month: String = ""
)
