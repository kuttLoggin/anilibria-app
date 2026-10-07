package ru.radiationx.anilibria.screen.details

import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.radiationx.anilibria.common.BaseCardsViewModel
import ru.radiationx.anilibria.common.CardItem
import ru.radiationx.anilibria.common.GradientBackgroundManager
import ru.radiationx.anilibria.common.LibriaCard
import ru.radiationx.anilibria.common.LibriaDetailsRow
import ru.radiationx.anilibria.common.LinkCard
import ru.radiationx.anilibria.common.LoadingCard
import ru.radiationx.anilibria.common.RowDiffCallback
import ru.radiationx.anilibria.extension.applyCard
import ru.radiationx.anilibria.extension.createCardsRowBy
import ru.radiationx.anilibria.ui.presenter.LibriaCardPresenter
import ru.radiationx.anilibria.ui.presenter.ReleaseDetailsPresenter
import ru.radiationx.anilibria.ui.presenter.cust.CustomListRowPresenter
import ru.radiationx.anilibria.ui.presenter.cust.CustomListRowViewHolder
import ru.radiationx.data.entity.domain.types.ReleaseId
import ru.radiationx.quill.QuillExtra
import ru.radiationx.quill.inject
import ru.radiationx.quill.viewModel
import ru.radiationx.shared.ktx.android.getExtraNotNull
import ru.radiationx.shared.ktx.android.putExtra
import ru.radiationx.shared.ktx.android.subscribeTo
import kotlin.coroutines.resume
import kotlin.math.ceil

data class DetailExtra(
    val id: ReleaseId,
) : QuillExtra

class DetailFragment : RowsSupportFragment() {

    companion object {
        private const val ARG_ID = "id"

        fun newInstance(releaseId: ReleaseId) = DetailFragment().putExtra {
            putParcelable(ARG_ID, releaseId)
        }
    }

    private val backgroundManager by inject<GradientBackgroundManager>()

    private var headerBackgroundImage: String? = null
    private val preparedCardPresenters = mutableSetOf<LibriaCardPresenter>()
    private val cardPreparationMutex = Mutex()

    private val argExtra by lazy {
        DetailExtra(id = getExtraNotNull(ARG_ID))
    }

    private val rowsPresenter by lazy {
        ClassPresenterSelector().apply {
            addClassPresenter(ListRow::class.java, CustomListRowPresenter())
            addClassPresenter(
                LibriaDetailsRow::class.java, ReleaseDetailsPresenter(
                    continueClickListener = headerViewModel::onContinueClick,
                    playClickListener = headerViewModel::onPlayClick,
                    favoriteClickListener = headerViewModel::onFavoriteClick,
                    descriptionClickListener = headerViewModel::onDescriptionClick,
                    otherClickListener = headerViewModel::onOtherClick
                )
            )
        }
    }
    private val rowsAdapter by lazy { ArrayObjectAdapter(rowsPresenter) }

    private val detailsViewModel by viewModel<DetailsViewModel> { argExtra }

    private val headerViewModel by viewModel<DetailHeaderViewModel> { argExtra }

    private val relatedViewModel by viewModel<DetailRelatedViewModel> { argExtra }

    private val recommendsViewModel by viewModel<DetailRecommendsViewModel> { argExtra }

    private fun getViewModel(rowId: Long): ViewModel? = when (rowId) {
        DetailsViewModel.RELEASE_ROW_ID -> headerViewModel
        DetailsViewModel.RELATED_ROW_ID -> relatedViewModel
        DetailsViewModel.RECOMMENDS_ROW_ID -> recommendsViewModel
        else -> null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerBackgroundImage = null
        preparedCardPresenters.clear()
        // Row updates must not pre-bind offscreen cards during the header transition.
        verticalGridView?.itemAnimator = null

        viewLifecycleOwner.lifecycle.addObserver(detailsViewModel)
        viewLifecycleOwner.lifecycle.addObserver(headerViewModel)
        viewLifecycleOwner.lifecycle.addObserver(relatedViewModel)
        viewLifecycleOwner.lifecycle.addObserver(recommendsViewModel)

        adapter = rowsAdapter

        setOnItemViewClickedListener { _, item, _, row ->
            val viewMode: BaseCardsViewModel? =
                getViewModel((row as ListRow).id) as? BaseCardsViewModel
            when (item) {
                is LinkCard -> viewMode?.onLinkCardClick()
                is LoadingCard -> viewMode?.onLoadingCardClick()
                is LibriaCard -> viewMode?.onLibriaCardClick(item)
            }
        }

        setOnItemViewSelectedListener { _, item, rowViewHolder, row ->
            if (row is ListRow) {
                headerBackgroundImage = null
                backgroundManager.applyCard(item)
            } else if (row is LibriaDetailsRow) {
                row.details?.image?.takeIf { it.isNotBlank() }?.let { image ->
                    if (headerBackgroundImage != image) {
                        headerBackgroundImage = image
                        applyImage(image)
                    }
                }
            }
            if (rowViewHolder is CustomListRowViewHolder) {
                when (item) {
                    is LibriaCard -> {
                        rowViewHolder.setDescription(item.title, item.description)
                    }

                    is LinkCard -> {
                        rowViewHolder.setDescription(item.title, "")
                    }

                    is LoadingCard -> {
                        rowViewHolder.setDescription(item.title, item.description)
                    }

                    else -> {
                        rowViewHolder.setDescription("", "")
                    }
                }
            }
        }

        val rowMap = mutableMapOf<Long, Row>()
        subscribeTo(detailsViewModel.rowListData) { rowList ->
            val rows = rowList.map { rowId ->
                val row = rowMap[rowId] ?: createRowBy(rowId, rowsAdapter, getViewModel(rowId)!!)
                rowMap[rowId] = row
                row
            }
            rowsAdapter.setItems(rows, RowDiffCallback)
        }
    }

    override fun onDestroyView() {
        preparedCardPresenters.forEach { it.clearPreparedViewHolders() }
        preparedCardPresenters.clear()
        super.onDestroyView()
    }

    private fun createRowBy(
        rowId: Long,
        rowsAdapter: ArrayObjectAdapter,
        viewModel: ViewModel,
    ): Row = when (rowId) {
        DetailsViewModel.RELEASE_ROW_ID -> createHeaderRowBy(
            rowId,
            rowsAdapter,
            viewModel as DetailHeaderViewModel
        )

        else -> createCardsRowBy(rowId, rowsAdapter, viewModel as BaseCardsViewModel).also { row ->
            subscribeTo(combine(viewModel.cardsData, headerViewModel.progressState) { cards, state ->
                cards.takeUnless { state.loadingProgress }.orEmpty()
            }) { cards -> prepareCards(row, cards) }
        }
    }

    private fun prepareCards(row: ListRow, cards: List<CardItem>) {
        val releaseCards = cards.filterIsInstance<LibriaCard>()
        val card = releaseCards.firstOrNull() ?: return
        val presenter = row.adapter.presenterSelector.getPresenter(card) as? LibriaCardPresenter
            ?: return
        if (!preparedCardPresenters.add(presenter)) return
        viewLifecycleOwner.lifecycleScope.launch {
            cardPreparationMutex.withLock {
                val grid = verticalGridView ?: return@withLock
                val cardWidth = resources.getDimension(ru.radiationx.anilibria.R.dimen.card_release_width)
                val width = grid.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
                val count = ceil(width / cardWidth.toDouble()).toInt()
                    .coerceIn(1, 8).coerceAtMost(releaseCards.size)
                repeat(count) {
                    awaitMainThreadIdle()
                    while (backgroundManager.isAnimating) {
                        delay(16L)
                        awaitMainThreadIdle()
                    }
                    if (!grid.isAttachedToWindow || grid.selectedPosition != 0) return@withLock
                    presenter.prepareViewHolder(grid, releaseCards[it], count)
                    delay(16L)
                }
            }
        }
    }

    private suspend fun awaitMainThreadIdle() = suspendCancellableCoroutine<Unit> { continuation ->
        val queue = Looper.myQueue()
        val handler = MessageQueue.IdleHandler {
            if (continuation.isActive) continuation.resume(Unit)
            false
        }
        queue.addIdleHandler(handler)
        continuation.invokeOnCancellation { queue.removeIdleHandler(handler) }
    }

    private fun createHeaderRowBy(
        rowId: Long,
        rowsAdapter: ArrayObjectAdapter,
        viewModel: DetailHeaderViewModel,
    ): Row {
        val row = LibriaDetailsRow(rowId)
        subscribeTo(viewModel.releaseData) {
            val position = rowsAdapter.indexOf(row)
            row.details = it
            rowsAdapter.notifyArrayItemRangeChanged(position, 1)
        }
        subscribeTo(viewModel.progressState) {
            val position = rowsAdapter.indexOf(row)
            row.state = it
            rowsAdapter.notifyArrayItemRangeChanged(position, 1)
        }
        return row
    }

    private fun applyImage(image: String) {
        backgroundManager.applyImage(image, colorModifier = {
            val hslColor = FloatArray(3)
            ColorUtils.colorToHSL(it, hslColor)
            hslColor[1] = (hslColor[1] + 0.05f).coerceAtMost(1.0f)
            hslColor[2] = (hslColor[2] + 0.05f).coerceAtMost(1.0f)
            ColorUtils.HSLToColor(hslColor)
        })
    }

}
