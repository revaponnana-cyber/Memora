package com.memora.app.app

import android.content.Context
import com.memora.app.data.MemoraDatabase
import com.memora.app.data.repository.RoomItemRepository
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.usecase.AddItemUseCase
import com.memora.app.domain.usecase.MoveItemUseCase
import com.memora.app.domain.usecase.RecordSentenceUseCase
import com.memora.app.domain.usecase.UndoMoveUseCase

/**
 * Tiny manual dependency container (no DI framework — lightweight on purpose).
 * Holds exactly one database and one repository; everything else is derived.
 */
class AppContainer(context: Context) {

    val itemRepository: ItemRepository = RoomItemRepository(MemoraDatabase.get(context))

    /** The sentence agent: language pipeline → identity → validated writes. */
    val recordSentence: RecordSentenceUseCase = RecordSentenceUseCase(itemRepository)

    val moveItem: MoveItemUseCase = MoveItemUseCase(itemRepository)
    val addItem: AddItemUseCase = AddItemUseCase(itemRepository)
    val undoMove: UndoMoveUseCase = UndoMoveUseCase(itemRepository)
}
