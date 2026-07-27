package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "actas")
data class ActaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val folioNumber: String = "",
    val dateTime: String = "",
    val location: String = "",
    val meetingType: String = "Ordinaria",
    val attendees: String = "",
    val absentees: String = "",
    val agenda: String = "",
    val discussion: String = "",
    val commitmentsJson: String = "[]",
    val secretary: String = "",
    val president: String = "",
    val attachedImagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): MeetingMinute {
        val commitmentsList = mutableListOf<MeetingCommitment>()
        if (commitmentsJson.isNotBlank()) {
            try {
                val array = JSONArray(commitmentsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    commitmentsList.add(
                        MeetingCommitment(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            agreement = obj.optString("agreement", ""),
                            responsible = obj.optString("responsible", ""),
                            dueDate = obj.optString("dueDate", "")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
        return MeetingMinute(
            id = id,
            folioNumber = folioNumber,
            dateTime = dateTime,
            location = location,
            meetingType = meetingType,
            attendees = attendees,
            absentees = absentees,
            agenda = agenda,
            discussion = discussion,
            commitments = commitmentsList,
            secretary = secretary,
            president = president,
            attachedImagePath = attachedImagePath,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainModel(minute: MeetingMinute): ActaEntity {
            val jsonArray = JSONArray()
            for (c in minute.commitments) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("agreement", c.agreement)
                    put("responsible", c.responsible)
                    put("dueDate", c.dueDate)
                }
                jsonArray.put(obj)
            }
            return ActaEntity(
                id = minute.id,
                folioNumber = minute.folioNumber,
                dateTime = minute.dateTime,
                location = minute.location,
                meetingType = minute.meetingType,
                attendees = minute.attendees,
                absentees = minute.absentees,
                agenda = minute.agenda,
                discussion = minute.discussion,
                commitmentsJson = jsonArray.toString(),
                secretary = minute.secretary,
                president = minute.president,
                attachedImagePath = minute.attachedImagePath,
                createdAt = minute.createdAt
            )
        }
    }
}
