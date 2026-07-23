package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.model.Mosaic
import com.example.data.model.Song
import com.example.data.model.TodayListHistory
import com.example.data.model.WorshipCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class WorshipRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val songDao = database.songDao()
    private val mosaicDao = database.mosaicDao()
    private val commandDao = database.commandDao()
    private val todayListHistoryDao = database.todayListHistoryDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allMosaics: Flow<List<Mosaic>> = mosaicDao.getAllMosaics()
    val allCommands: Flow<List<WorshipCommand>> = commandDao.getAllCommands()
    val todayListHistory: Flow<List<TodayListHistory>> = todayListHistoryDao.getAllHistory()

    suspend fun getTodayListHistoryById(id: Long): TodayListHistory? = withContext(Dispatchers.IO) {
        todayListHistoryDao.getHistoryById(id)
    }

    suspend fun insertTodayListHistory(history: TodayListHistory): Long = withContext(Dispatchers.IO) {
        todayListHistoryDao.insertHistory(history)
    }

    suspend fun deleteTodayListHistory(history: TodayListHistory) = withContext(Dispatchers.IO) {
        todayListHistoryDao.deleteHistory(history)
    }

    suspend fun deleteTodayListHistoryById(id: Long) = withContext(Dispatchers.IO) {
        todayListHistoryDao.deleteHistoryById(id)
    }

    suspend fun insertCommand(command: WorshipCommand): Long = withContext(Dispatchers.IO) {
        commandDao.insertCommand(command)
    }

    suspend fun updateCommand(command: WorshipCommand) = withContext(Dispatchers.IO) {
        commandDao.updateCommand(command)
    }

    suspend fun deleteCommand(command: WorshipCommand) = withContext(Dispatchers.IO) {
        commandDao.deleteCommand(command)
    }

    suspend fun resetCommandsToDefault() = withContext(Dispatchers.IO) {
        commandDao.deleteAllCommands()
        val defaults = listOf(
            WorshipCommand(text = "🔵 REPETIMOS CORO", displayOrder = 0),
            WorshipCommand(text = "🟢 SOLO PIANO", displayOrder = 1),
            WorshipCommand(text = "SOLO GUITARRA", displayOrder = 2),
            WorshipCommand(text = "🟡 TODA LA BANDA", displayOrder = 3),
            WorshipCommand(text = "🔴 TERMINAMOS", displayOrder = 4)
        )
        commandDao.insertCommands(defaults)
    }

    suspend fun getSongById(id: Long): Song? = withContext(Dispatchers.IO) {
        songDao.getSongById(id)
    }

    fun getSongByIdFlow(id: Long): Flow<Song?> = songDao.getSongByIdFlow(id)

    suspend fun insertSong(song: Song): Long = withContext(Dispatchers.IO) {
        songDao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        songDao.insertSongs(songs)
    }

    suspend fun updateSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.updateSong(song)
    }

    suspend fun updateSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        songDao.updateSongs(songs)
    }

    suspend fun deleteSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.deleteSong(song)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun updateBatchCategory(ids: List<Long>, category: String) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) songDao.updateBatchCategory(ids, category)
    }

    suspend fun updateBatchKey(ids: List<Long>, key: String) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) songDao.updateBatchKey(ids, key)
    }

    suspend fun deleteBatchSongs(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) songDao.deleteBatchSongs(ids)
    }

    suspend fun getMosaicById(id: Long): Mosaic? = withContext(Dispatchers.IO) {
        mosaicDao.getMosaicById(id)
    }

    suspend fun insertMosaic(mosaic: Mosaic): Long = withContext(Dispatchers.IO) {
        mosaicDao.insertMosaic(mosaic)
    }

    suspend fun updateMosaic(mosaic: Mosaic) = withContext(Dispatchers.IO) {
        mosaicDao.updateMosaic(mosaic)
    }

    suspend fun deleteMosaic(mosaic: Mosaic) = withContext(Dispatchers.IO) {
        mosaicDao.deleteMosaic(mosaic)
    }

    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val currentSongs = songDao.getAllSongs().firstOrNull()
        if (currentSongs.isNullOrEmpty()) {
            val defaultSongs = listOf(
                Song(
                    title = "Cuán Grande es Él",
                    category = "Himnos",
                    key = "G",
                    lyrics = """[G]Señor, mi Dios, al [C]contemplar los cielos
[G]El firmamento y [D]las estrellas [G]mil
[G]Al oír tu voz en [C]los potentes truenos
[G]Y ver brillar al [D]sol en su cen[G]it

Coro:
Mi co[G]razón entona [C]la can[G]ción
¡Cuán grande es [Am]Él! [D]¡Cuán grande es [G]Él!
Mi co[G]razón entona [C]la can[G]ción
¡Cuán grande es [Am]Él! [D]¡Cuán grande es [G]Él!

Estrofa 2:
Al re[G]correr los montes [C]y los valles
Y ver las [G]bellas flores [D]al pa[G]sar
Al escuchar el [C]canto de las aves
Y el murmu[G]rar del claro [D]manan[G]tial""",
                    notes = "Himno tradicional de adoración y grandeza. Mantener un ritmo lento y solemne.",
                    displayOrder = 0
                ),
                Song(
                    title = "Dios Incomparable",
                    category = "Adoración",
                    key = "G",
                    lyrics = """[G]Dios de mi corazón, [D]Dios de mi salvación
[C]En tu presencia hay plenitud, [Em]seguridad y [D]amor
[G]Cantaré de tu bondad, [D]por la eternidad
[C]Grande eres Tú, Señor, [Em]digno de ado[D]rar

Coro:
[G]Dios incomparable, [D]eres el mismo ayer y hoy
[Em]Tu fidelidad durará [C]por siempre, Señor
[G]En la tormenta me sostendrás, [D]tu gracia me bastará
[Em]Mi roca fuerte y libertador, [C]te adoraré con todo mi ser

Puente:
[Em]Tu gracia me salvó, [C]tu amor me redimió
[G]Toda la gloria sea a [D]Tí, Jesús""",
                    notes = "Canción de adoración contemporánea. El puente sube de intensidad.",
                    displayOrder = 1
                ),
                Song(
                    title = "Te Alabaré, Mi Buen Señor",
                    category = "Alabanzas",
                    key = "E",
                    lyrics = """[E]Te alabaré, mi buen Señor, [A]con todo el corazón
[E]Cantaré tus maravillas, [B]gritaré de tu amor
[E]En la prueba o el dolor, [A]Tú eres mi porción
[E]Mi escudo, mi refugio, mi [B]fuerte salva[E]ción

Coro:
[E]¡Te alabaré! ¡Te bendeciré!
[A]Por siempre reinarás, oh Rey
[E]La muerte venciste en la cruz
[B]Dándonos vida y luz, Jesús
[E]¡Te alabaré! ¡Te bendeciré!
[A]Mi corona a tus pies rendiré
[E]Santo, Santo eres [B]Tú, mi Se[E]ñor""",
                    notes = "Tema alegre y dinámico. Ideal para comenzar el culto.",
                    displayOrder = 2
                ),
                Song(
                    title = "Tu Fidelidad",
                    category = "Adoración",
                    key = "C",
                    lyrics = """[C]Tu fidelidad es [Dm]grande
[G]Tu fidelidad, incompa[C]rable es
[Am]Nadie como Tú, ben[Dm]dito Dios
[F]Grande es tu [G]fidelid[C]ad

Coro:
[G]Tu fidelidad es [C]grande
[C]Tu fidelidad es [F]grande
[F]Nadie como [G]Tú, bendito [Em]Dios [Am]
[Dm]Grande es tu [G]fidelid[C]ad""",
                    notes = "Canto de adoración muy conocido. Mantener un ambiente íntimo y suave.",
                    displayOrder = 3
                ),
                Song(
                    title = "Gracia Sublime Es",
                    category = "Alabanzas",
                    key = "G",
                    lyrics = """[G]¿Quién rompe el poder del pe[C]cado?
Su amor es fuerte y para [Em]siempre
El Rey de gloria, [D]el Rey de reyes

Coro:
[G]Gracia sublime es, [C]tu perfecto amor
[Em]Tomaste mi lugar, [D]llevaste mi cruz
[G]Tu vida entregaste allí, [C]y libre ahora soy
[Em]¡Oh, Jesús! [D]Te adoro por lo que hi[G]ciste por mí

Estrofa 2:
[G]¿Quién trae orden al ca[C]os?
¿Quién nos adopta como [Em]hijos?
El Rey de gloria, [D]el Rey de reyes""",
                    notes = "Canción alegre. El coro se canta con mucha fuerza.",
                    displayOrder = 4
                )
            )
            songDao.insertSongs(defaultSongs)
        }

        val currentCommands = commandDao.getAllCommands().firstOrNull()
        if (currentCommands.isNullOrEmpty()) {
            val defaults = listOf(
                WorshipCommand(text = "🔵 REPETIMOS CORO", displayOrder = 0),
                WorshipCommand(text = "🟢 SOLO PIANO", displayOrder = 1),
                WorshipCommand(text = "SOLO GUITARRA", displayOrder = 2),
                WorshipCommand(text = "🟡 TODA LA BANDA", displayOrder = 3),
                WorshipCommand(text = "🔴 TERMINAMOS", displayOrder = 4)
            )
            commandDao.insertCommands(defaults)
        }
    }
}
