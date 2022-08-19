package com.zionhuang.music.ui.listeners

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.zionhuang.innertube.models.BrowseEndpoint
import com.zionhuang.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs
import com.zionhuang.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig
import com.zionhuang.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.zionhuang.innertube.models.NavigationEndpoint
import com.zionhuang.music.constants.MediaConstants.EXTRA_MEDIA_METADATA_ITEMS
import com.zionhuang.music.constants.MediaConstants.EXTRA_SONG
import com.zionhuang.music.constants.MediaSessionConstants.COMMAND_ADD_TO_QUEUE
import com.zionhuang.music.constants.MediaSessionConstants.COMMAND_PLAY_NEXT
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.extensions.show
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.MediaSessionConnection
import com.zionhuang.music.repos.SongRepository
import com.zionhuang.music.ui.fragments.dialogs.ChoosePlaylistDialog
import com.zionhuang.music.ui.fragments.dialogs.EditSongDialog
import com.zionhuang.music.utils.NavigationEndpointHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface ISongMenuListener {
    fun editSong(song: Song)
    fun playNext(songs: List<Song>)
    fun addToQueue(songs: List<Song>)
    fun addToPlaylist(songs: List<Song>)
    fun download(songs: List<Song>)
    fun removeDownload(songs: List<Song>)
    fun viewArtist(song: Song)
    fun viewAlbum(song: Song)
    fun share(song: Song)
    fun delete(songs: List<Song>)

    fun playNext(song: Song) = playNext(listOf(song))
    fun addToQueue(song: Song) = addToQueue(listOf(song))
    fun addToPlaylist(song: Song) = addToPlaylist(listOf(song))
    fun download(song: Song) = download(listOf(song))
    fun removeDownload(song: Song) = removeDownload(listOf(song))
    fun delete(song: Song) = delete(listOf(song))
}

class SongMenuListener(private val fragment: Fragment) : ISongMenuListener {
    val context: Context
        get() = fragment.requireContext()

    override fun editSong(song: Song) {
        EditSongDialog().apply {
            arguments = bundleOf(EXTRA_SONG to song)
        }.show(context)
    }

    override fun playNext(songs: List<Song>) {
        MediaSessionConnection.mediaController?.sendCommand(
            COMMAND_PLAY_NEXT,
            bundleOf(EXTRA_MEDIA_METADATA_ITEMS to songs.map { it.toMediaMetadata() }.toTypedArray()),
            null
        )
    }

    override fun addToQueue(songs: List<Song>) {
        MediaSessionConnection.mediaController?.sendCommand(
            COMMAND_ADD_TO_QUEUE,
            bundleOf(EXTRA_MEDIA_METADATA_ITEMS to songs.map { it.toMediaMetadata() }.toTypedArray()),
            null
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addToPlaylist(songs: List<Song>) {
        ChoosePlaylistDialog {
            GlobalScope.launch {
                SongRepository.addToPlaylist(it, songs)
            }
        }.show(fragment.childFragmentManager, null)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun download(songs: List<Song>) {
        GlobalScope.launch {
            SongRepository.downloadSongs(songs.map { it.song })
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun removeDownload(songs: List<Song>) {
        GlobalScope.launch {
            SongRepository.removeDownloads(songs)
        }
    }

    override fun viewArtist(song: Song) {
        if (song.artists.isNotEmpty()) {
            NavigationEndpointHandler(fragment).handle(NavigationEndpoint(
                browseEndpoint = BrowseEndpoint(
                    browseId = song.artists[0].id
                )
            ))
        }
    }

    override fun viewAlbum(song: Song) {
        if (song.song.albumId != null) {
            NavigationEndpointHandler(fragment).handle(NavigationEndpoint(
                browseEndpoint = BrowseEndpoint(
                    browseId = song.song.albumId,
                    browseEndpointContextSupportedConfigs = BrowseEndpointContextSupportedConfigs(
                        browseEndpointContextMusicConfig = BrowseEndpointContextMusicConfig(
                            pageType = MUSIC_PAGE_TYPE_ALBUM
                        )
                    )
                )
            ))
        }
    }

    override fun share(song: Song) {
        val intent = Intent().apply {
            action = ACTION_SEND
            type = "text/plain"
            putExtra(EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
        }
        fragment.startActivity(Intent.createChooser(intent, null))
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun delete(songs: List<Song>) {
        GlobalScope.launch {
            SongRepository.deleteSongs(songs)
        }
    }
}