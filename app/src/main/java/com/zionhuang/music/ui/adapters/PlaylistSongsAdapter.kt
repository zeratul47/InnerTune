package com.zionhuang.music.ui.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SELECTION_CHANGED_MARKER
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.zionhuang.music.R
import com.zionhuang.music.constants.Constants.HEADER_ITEM_ID
import com.zionhuang.music.constants.Constants.TYPE_HEADER
import com.zionhuang.music.constants.Constants.TYPE_ITEM
import com.zionhuang.music.constants.MediaConstants.STATE_DOWNLOADING
import com.zionhuang.music.constants.ORDER_ARTIST
import com.zionhuang.music.constants.ORDER_CREATE_DATE
import com.zionhuang.music.constants.ORDER_NAME
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.extensions.inflateWithBinding
import com.zionhuang.music.models.DownloadProgress
import com.zionhuang.music.models.base.IMutableSortInfo
import com.zionhuang.music.ui.listeners.ISongMenuListener
import com.zionhuang.music.ui.viewholders.SongHeaderViewHolder
import com.zionhuang.music.ui.viewholders.SongViewHolder
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.text.DateFormat

class PlaylistSongsAdapter : PagingDataAdapter<Song, RecyclerView.ViewHolder>(SongItemComparator()), PopupTextProvider {
    var popupMenuListener: ISongMenuListener? = null
    var sortInfo: IMutableSortInfo? = null
    var downloadInfo: LiveData<Map<String, DownloadProgress>>? = null
    var tracker: SelectionTracker<String>? = null
    var itemTouchHelper: ItemTouchHelper? = null

    private val moves: MutableList<Pair<Int, Int>> = mutableListOf()
    var onProcessMove: ((List<Pair<Int, Int>>) -> Unit)? = null

    fun moveItem(from: Int, to: Int) {
        moves.add(Pair(from, to))
        notifyItemMoved(from, to)
    }

    fun processMove() {
        onProcessMove?.let {
            it(moves.toList())
            moves.clear()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> getItem(position)?.let { song ->
                holder.bind(song, tracker?.isSelected(song.song.id))
                if (song.song.downloadState == STATE_DOWNLOADING) {
                    downloadInfo?.value?.get(song.song.id)?.let { info ->
                        holder.setProgress(info, false)
                    }
                }
            }
            is SongHeaderViewHolder -> holder.bind(itemCount - 1)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        when (holder) {
            is SongViewHolder -> {
                if (payloads.isEmpty()) {
                    onBindViewHolder(holder, position)
                } else when (val payload = payloads[0]) {
                    SELECTION_CHANGED_MARKER -> holder.onSelectionChanged(
                        tracker?.isSelected(
                            holder.binding.song?.song?.id
                        )
                    )
                    is Song -> holder.bind(payload)
                    is DownloadProgress -> holder.setProgress(payload)
                }
            }
            is SongHeaderViewHolder -> holder.bind(itemCount - 1)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> SongHeaderViewHolder(parent.inflateWithBinding(R.layout.item_song_header), sortInfo!!)
            TYPE_ITEM -> SongViewHolder(parent.inflateWithBinding(R.layout.item_song), popupMenuListener)
            else -> throw IllegalArgumentException("Unexpected view type.")
        }

    fun getItemByPosition(position: Int): Song? = getItem(position)

    fun setProgress(id: String, progress: DownloadProgress) {
        snapshot().items.forEachIndexed { index, song ->
            if (song.song.id == id) {
                notifyItemChanged(index, progress)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position)?.song?.id == HEADER_ITEM_ID) TYPE_HEADER else TYPE_ITEM

    private val dateFormat = DateFormat.getDateInstance()

    override fun getPopupText(position: Int): String =
        if (getItemViewType(position) == TYPE_HEADER) "#"
        else getItem(position)?.let {
            when (sortInfo!!.type) {
                ORDER_CREATE_DATE -> dateFormat.format(it.song.createDate)
                ORDER_NAME -> it.song.title[0].toString()
                ORDER_ARTIST -> it.artists.joinToString { it.name }
                else -> it.song.title[0].toString()
            }
        } ?: ""

    class SongItemComparator : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem.song.id == newItem.song.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem == newItem
        override fun getChangePayload(oldItem: Song, newItem: Song): Song = newItem
    }
}