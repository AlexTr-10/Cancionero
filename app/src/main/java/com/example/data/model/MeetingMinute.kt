package com.example.data.model

import java.util.UUID

data class MeetingCommitment(
    val id: String = UUID.randomUUID().toString(),
    val agreement: String = "",
    val responsible: String = "",
    val dueDate: String = ""
)

data class MeetingMinute(
    val id: String = UUID.randomUUID().toString(),
    val folioNumber: String = "", // Ej: "Acta N° 001 - 2026"
    val dateTime: String = "", // Fecha y Hora (Ej: "26/07/2026 19:30")
    val location: String = "", // Lugar / Sede
    val meetingType: String = "Ordinaria", // Ordinaria, Extraordinaria, Asamblea, Comité
    val attendees: String = "", // Lista de Asistentes
    val absentees: String = "", // Lista de Ausentes
    val agenda: String = "", // Orden del Día (Puntos a tratar)
    val discussion: String = "", // Desarrollo / Debate de la Sesión
    val commitments: List<MeetingCommitment> = emptyList(), // Compromisos y Tareas
    val secretary: String = "", // Firmante Secretario/a
    val president: String = "", // Firmante Presidente/Pastor
    val attachedImagePath: String = "", // Ruta del documento digitalizado
    val createdAt: Long = System.currentTimeMillis()
)
