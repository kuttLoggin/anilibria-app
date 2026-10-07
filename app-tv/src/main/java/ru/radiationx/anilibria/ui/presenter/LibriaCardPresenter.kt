package ru.radiationx.anilibria.ui.presenter

import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import ru.radiationx.anilibria.R
import ru.radiationx.anilibria.common.LibriaCard
import ru.radiationx.shared_app.imageloader.showImageUrl

class LibriaCardPresenter : Presenter() {

    private val preparedHolders = java.util.ArrayDeque<ViewHolder>()
    private val preparedItems = java.util.IdentityHashMap<ViewHolder, LibriaCard>()

    // Called one card at a time while the release header is idle; holders stay unattached.
    fun prepareViewHolder(parent: ViewGroup, card: LibriaCard, limit: Int) {
        if (preparedHolders.size < limit) {
            val holder = createViewHolder(parent)
            onBindViewHolder(holder, card)
            val cardView = holder.view as ImageCardView
            val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            cardView.measure(unspecified, unspecified)
            cardView.layout(0, 0, cardView.measuredWidth, cardView.measuredHeight)
            preparedHolders.addLast(holder)
            preparedItems[holder] = card
        }
    }

    fun clearPreparedViewHolders() {
        preparedHolders.forEach { holder ->
            onUnbindViewHolder(holder)
            (holder.view as ImageCardView).mainImageView?.showImageUrl(null)
        }
        preparedHolders.clear()
        preparedItems.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return preparedHolders.pollFirst() ?: createViewHolder(parent)
    }

    private fun createViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        return LibriaCardViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        item ?: return
        item as LibriaCard
        viewHolder as LibriaCardViewHolder
        // Reuse the preparation only for the same snapshot; changed/reordered data binds normally.
        if (preparedItems.remove(viewHolder) !== item) {
            viewHolder.bind(item)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        viewHolder as LibriaCardViewHolder
        viewHolder.unbind()
    }
}

class LibriaCardViewHolder(
    private val containerView: ImageCardView,
) : Presenter.ViewHolder(containerView) {

    private val cardHeight by lazy {
        containerView.context.resources.getDimension(R.dimen.card_height).toInt()
    }
    private val cardReleaseWidth by lazy {
        containerView.context.resources.getDimension(R.dimen.card_release_width).toInt()
    }
    private val cardYoutubeWidth by lazy {
        containerView.context.resources.getDimension(R.dimen.card_youtube_width).toInt()
    }

    fun bind(item: LibriaCard) {
        when (item.type) {
            is LibriaCard.Type.Release -> containerView.setMainImageDimensions(
                cardReleaseWidth,
                cardHeight
            )

            is LibriaCard.Type.Youtube -> containerView.setMainImageDimensions(
                cardYoutubeWidth,
                cardHeight
            )
        }
        containerView.mainImageView?.showImageUrl(item.image)
    }

    fun unbind() {

    }
}
