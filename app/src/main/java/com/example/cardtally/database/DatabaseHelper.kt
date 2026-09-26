package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cardtally.BuildConfig
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatSession
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.model.RecurringRecord
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.Money
import com.example.cardtally.util.RecurringScheduleCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(
    private val appContext: Context
) : SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val recurringRepository by lazy {
        RecurringRecordRepository(
            readableDatabase = { readableDatabase },
            writableDatabase = { writableDatabase },
            currentLedgerId = ::currentLedgerId,
            validateCategory = { db, recurring ->
                val categoryId = requireNotNull(recurring.categoryId)
                val category = getCategoryByIdInternal(db, categoryId) ?: error("Category missing")
                require(recurring.type != 2 && category.type == recurring.type && !categoryHasChildren(db, categoryId))
            },
            isAssetAvailable = { db, ledgerId, assetId ->
                db.rawQuery(
                    "SELECT 1 FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
                    arrayOf(ledgerId.toString(), assetId.toString())
                ).use { it.moveToFirst() }
            },
            columns = RECURRING_RECORD_COLUMNS
        )
    }
    private val categoryHierarchyValidator by lazy {
        CategoryHierarchyValidator(
            findCategory = ::getCategoryByIdInternal,
            configuredMaxDepth = { CategoryHierarchySettingsHelper.getCategoryMaxDepth(appContext) },
            throwError = { throw CategoryOperationException(it) }
        )
    }
    private val categoryReadRepository by lazy {
        CategoryReadRepository(
            readableDatabase = { readableDatabase },
            columns = CATEGORY_READ_COLUMNS
        )
    }
    private val categoryWriteRepository by lazy {
        CategoryWriteRepository(
            writableDatabase = { writableDatabase },
            validateParent = ::validateParentAssignment,
            findExistingId = { db, category ->
                findCategoryId(db, category.name, category.type, category.parentId)
            },
            hasChildren = ::categoryHasChildren,
            isReferencedByRecord = ::categoryIsReferencedByRecordId,
            throwError = { throw CategoryOperationException(it) },
            columns = CATEGORY_WRITE_COLUMNS
        )
    }
    private val aiChatRepository by lazy {
        AiChatRepository(
            readableDatabase = { readableDatabase },
            writableDatabase = { writableDatabase },
            columns = AI_CHAT_COLUMNS
        )
    }
    private val recordStatisticsRepository by lazy {
        RecordStatisticsRepository(
            readableDatabase = { readableDatabase },
            currentLedgerId = ::currentLedgerId,
            columns = RECORD_STATISTICS_COLUMNS
        )
    }
    private val recordReadRepository by lazy {
        RecordReadRepository(
            readableDatabase = { readableDatabase },
            currentLedgerId = ::currentLedgerId,
            fromCursor = ::createRecordFromCursor,
            columns = RECORD_READ_COLUMNS,
            pageLimit = MAX_RECORD_QUERY_LIMIT
        )
    }
    private val recordWriteRepository by lazy {
        RecordWriteRepository(
            writableDatabase = { writableDatabase },
            currentLedgerId = ::currentLedgerId,
            validateRecord = ::validateRecordForWrite,
            findRecord = ::getRecordByIdInternal,
            recordFromCursor = ::createRecordFromCursor,
            recordValues = ::createRecordValues,
            undoValues = ::createRecordDeletionUndoValues,
            applyAssetEffect = recordAssetBalanceRepository::applyRecordEffect,
            adjustAssetBalance = recordAssetBalanceRepository::adjustBalance,
            deleteRecordPhotos = ::deleteRecordPhotoUris,
            columns = RECORD_WRITE_COLUMNS
        )
    }
    private val recordWriteValidator by lazy {
        RecordWriteValidator(
            findCategory = ::getCategoryByIdInternal,
            categoryHasChildren = ::categoryHasChildren,
            isAvailableAsset = { db, assetId ->
                db.rawQuery(
                    "SELECT 1 FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_ID = ? " +
                        "AND $COLUMN_ASSET_IS_ARCHIVED = 0 LIMIT 1",
                    arrayOf(assetId.toString())
                ).use { it.moveToFirst() }
            }
        )
    }
    private val recordAssetBalanceRepository by lazy {
        RecordAssetBalanceRepository(
            assetScope = ::assetScope,
            columns = RECORD_ASSET_BALANCE_COLUMNS
        )
    }
    private val assetReadRepository by lazy {
        AssetReadRepository(
            readableDatabase = { readableDatabase },
            activeAssetScope = ::assetScope,
            columns = ASSET_READ_COLUMNS
        )
    }
    private val assetWriteRepository by lazy {
        AssetWriteRepository(
            writableDatabase = { writableDatabase },
            currentLedgerId = ::currentLedgerId,
            activeAssetScope = ::assetScope,
            isAssetNameReferenced = ::assetNameIsReferenced,
            throwError = { throw AssetOperationException(it) },
            columns = ASSET_WRITE_COLUMNS
        )
    }
    private val ledgerReadRepository by lazy {
        LedgerReadRepository(
            readableDatabase = { readableDatabase },
            columns = LEDGER_READ_COLUMNS
        )
    }
    private val ledgerWriteRepository by lazy {
        LedgerWriteRepository(
            writableDatabase = { writableDatabase },
            currentLedgerId = ::currentLedgerId,
            getLedgers = ::getLedgers,
            getMasterLedgerId = ::getMasterLedgerId,
            getSharedSourceLedgerId = ::getSharedSourceLedgerId,
            columns = LEDGER_WRITE_COLUMNS
        )
    }
    private val schemaUpgradeManager by lazy {
        DatabaseSchemaUpgradeManager(
            isVerificationBuild = BuildConfig.APPLICATION_ID.endsWith(".verification"),
            resetVerificationDatabase = ::resetAllDataForVerification,
            ensureColumn = ::ensureColumn,
            initializeAssetSortOrders = ::initializeAssetSortOrders,
            createRecurringTableAndIndex = { db ->
                db.execSQL(CREATE_TABLE_RECURRING_RECORDS)
                db.execSQL(CREATE_INDEX_RECURRING_DUE)
            },
            normalizeLegacyAssetCategory = { db ->
                db.execSQL(
                    "UPDATE $TABLE_ASSETS SET $COLUMN_ASSET_CATEGORY_LABEL = ?, " +
                        "$COLUMN_ASSET_CATEGORY_ICON_NAME = ? WHERE $COLUMN_ASSET_CATEGORY_LABEL = ?",
                    arrayOf("其他信用", "tabler_credit_card", "借呗 / 其他信用")
                )
            },
            recurringScheduleColumns = listOf(
                DatabaseSchemaUpgradeManager.ColumnMigration(
                    TABLE_RECURRING_RECORDS,
                    COLUMN_RECURRING_WEEKLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_WEEKLY_DAY INTEGER"
                ),
                DatabaseSchemaUpgradeManager.ColumnMigration(
                    TABLE_RECURRING_RECORDS,
                    COLUMN_RECURRING_MONTHLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_MONTHLY_DAY INTEGER"
                ),
                DatabaseSchemaUpgradeManager.ColumnMigration(
                    TABLE_RECURRING_RECORDS,
                    COLUMN_RECURRING_YEARLY_MONTH,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_YEARLY_MONTH INTEGER"
                ),
                DatabaseSchemaUpgradeManager.ColumnMigration(
                    TABLE_RECURRING_RECORDS,
                    COLUMN_RECURRING_YEARLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_YEARLY_DAY INTEGER"
                ),
                DatabaseSchemaUpgradeManager.ColumnMigration(
                    TABLE_RECURRING_RECORDS,
                    COLUMN_RECURRING_INTERVAL_DAYS,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_INTERVAL_DAYS INTEGER"
                )
            ),
            recurringTableRebuild = DatabaseSchemaUpgradeManager.TableRebuildMigration(
                table = TABLE_RECURRING_RECORDS,
                createTableSql = CREATE_TABLE_RECURRING_RECORDS,
                copiedColumns = listOf(
                    COLUMN_RECURRING_ID,
                    COLUMN_LEDGER_ID,
                    COLUMN_RECURRING_TYPE,
                    COLUMN_RECURRING_NAME,
                    COLUMN_RECURRING_AMOUNT,
                    COLUMN_RECURRING_CATEGORY_ID,
                    COLUMN_RECURRING_CATEGORY_NAME,
                    COLUMN_RECURRING_CATEGORY_PATH,
                    COLUMN_RECURRING_ASSET_ID,
                    COLUMN_RECURRING_ASSET_SOURCE,
                    COLUMN_RECURRING_NOTE,
                    COLUMN_RECURRING_FREQUENCY,
                    COLUMN_RECURRING_START_DATE,
                    COLUMN_RECURRING_END_DATE,
                    COLUMN_RECURRING_ENABLED,
                    COLUMN_RECURRING_NEXT_DUE_DATE
                )
            ),
            createPerformanceIndices = ::createPerformanceIndices,
            config = DatabaseSchemaUpgradeManager.Config(
                assetTable = TABLE_ASSETS,
                assetSortOrderColumn = COLUMN_ASSET_SORT_ORDER,
                assetSortOrderSql = "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_SORT_ORDER INTEGER NOT NULL DEFAULT 0",
                recurringDueIndexSql = CREATE_INDEX_RECURRING_DUE
            )
        )
    }
    private val schemaCreator by lazy {
        DatabaseSchemaCreator(
            createTablesBeforeDefaultLedger = listOf(
                CREATE_TABLE_LEDGERS,
                CREATE_TABLE_LEDGER_SHARED_ASSETS,
                CREATE_TABLE_LEDGER_SHARED_LEDGERS
            ),
            defaultLedgerTable = TABLE_LEDGERS,
            defaultLedgerNameColumn = COLUMN_LEDGER_NAME,
            defaultLedgerSubtitleColumn = COLUMN_LEDGER_SUBTITLE,
            defaultLedgerOrderColumn = COLUMN_LEDGER_SORT_ORDER,
            createTablesAfterDefaultLedger = listOf(
                CREATE_TABLE_RECORDS,
                CREATE_TABLE_CATEGORIES,
                CREATE_TABLE_ASSETS,
                CREATE_TABLE_RECORD_DELETION_UNDO,
                CREATE_TABLE_AI_CHAT_SESSIONS,
                CREATE_TABLE_AI_CHAT_MESSAGES,
                CREATE_TABLE_RECURRING_RECORDS
            ),
            createIndices = listOf(
                CREATE_INDEX_RECORDS_LEDGER_DATE,
                CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE,
                CREATE_INDEX_RECORDS_SOURCE_ASSET,
                CREATE_INDEX_RECORDS_DESTINATION_ASSET,
                CREATE_INDEX_CATEGORIES_TYPE_PARENT,
                CREATE_INDEX_ASSETS_LEDGER_ARCHIVED,
                CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER,
                CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT,
                CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT,
                CREATE_INDEX_RECURRING_DUE
            ),
            insertDefaultCategories = categoryDefaultsSeeder::seed
        )
    }
    private val categoryDefaultsSeeder by lazy {
        CategoryDefaultsSeeder(CATEGORY_DEFAULTS_COLUMNS)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    enum class CategoryOperationError {
        PARENT_NOT_FOUND,
        PARENT_TYPE_MISMATCH,
        SELF_PARENT,
        DESCENDANT_CYCLE,
        MAX_DEPTH_EXCEEDED,
        HAS_CHILDREN,
        IN_USE_BY_RECORDS
    }

    class CategoryOperationException(
        val error: CategoryOperationError
    ) : IllegalArgumentException(error.name)

    enum class AssetOperationError {
        NOT_ARCHIVED,
        IN_USE_BY_RECORDS
    }

    class AssetOperationException(
        val error: AssetOperationError
    ) : IllegalArgumentException(error.name)

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private val RECURRING_PROCESS_LOCK = Any()
        /** v35 splits the combined credit asset label. */
        private const val DATABASE_VERSION = 37
        const val MAX_RECORD_QUERY_LIMIT = 200
        const val RECORD_UNDO_WINDOW_MS = 10_000L

        private const val TABLE_RECORDS = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_RECORD_CATEGORY_ID = "category_id"
        private const val COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT = "category_name_snapshot"
        private const val COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT = "category_path_snapshot"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_RECORD_FEE = "fee"
        private const val COLUMN_RECORD_ASSET_ID = "asset_id"
        private const val COLUMN_RECORD_DESTINATION_ASSET_ID = "destination_asset_id"
        private const val COLUMN_ASSET_SOURCE = "asset_source"
        private const val COLUMN_DESTINATION_ASSET_SOURCE = "destination_asset_source"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_PHOTO_URIS = "photo_uris"
        private const val PHOTO_URI_SEPARATOR = "|"
        private const val COLUMN_SORT_ORDER = "sort_order"
        private const val COLUMN_LEDGER_ID = "ledger_id"
        private val RECORD_SQL_COLUMNS = RecordSqlMapper.Columns(
            id = COLUMN_ID,
            ledgerId = COLUMN_LEDGER_ID,
            date = COLUMN_DATE,
            amount = COLUMN_AMOUNT,
            category = COLUMN_CATEGORY,
            categoryId = COLUMN_RECORD_CATEGORY_ID,
            categoryNameSnapshot = COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT,
            categoryPathSnapshot = COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT,
            type = COLUMN_TYPE,
            description = COLUMN_DESCRIPTION,
            fee = COLUMN_RECORD_FEE,
            assetId = COLUMN_RECORD_ASSET_ID,
            destinationAssetId = COLUMN_RECORD_DESTINATION_ASSET_ID,
            assetSource = COLUMN_ASSET_SOURCE,
            destinationAssetSource = COLUMN_DESTINATION_ASSET_SOURCE,
            photoUri = COLUMN_PHOTO_URI,
            photoUris = COLUMN_PHOTO_URIS,
            sortOrder = COLUMN_SORT_ORDER,
            photoUriSeparator = PHOTO_URI_SEPARATOR
        )
        private val RECORD_READ_COLUMNS = RecordReadRepository.Columns(
            table = TABLE_RECORDS,
            id = COLUMN_ID,
            ledgerId = COLUMN_LEDGER_ID,
            date = COLUMN_DATE,
            sortOrder = COLUMN_SORT_ORDER,
            sourceAssetId = COLUMN_RECORD_ASSET_ID,
            destinationAssetId = COLUMN_RECORD_DESTINATION_ASSET_ID,
            assetSource = COLUMN_ASSET_SOURCE
        )
        private val RECORD_WRITE_COLUMNS = RecordWriteRepository.Columns(
            recordsTable = TABLE_RECORDS,
            recordId = COLUMN_ID,
            recordLedgerId = COLUMN_LEDGER_ID,
            recordDate = COLUMN_DATE,
            recordSortOrder = COLUMN_SORT_ORDER,
            categoryNameSnapshot = COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT,
            categoryPathSnapshot = COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT,
            undoTable = TABLE_RECORD_DELETION_UNDO,
            undoToken = COLUMN_UNDO_TOKEN,
            undoExpiresAt = COLUMN_UNDO_EXPIRES_AT,
            undoBalanceDelta = COLUMN_UNDO_BALANCE_DELTA
        )

        private const val TABLE_RECURRING_RECORDS = "recurring_records"
        private const val COLUMN_RECURRING_ID = "id"
        private const val COLUMN_RECURRING_TYPE = "type"
        private const val COLUMN_RECURRING_NAME = "name"
        private const val COLUMN_RECURRING_AMOUNT = "amount"
        private const val COLUMN_RECURRING_CATEGORY_ID = "category_id"
        private const val COLUMN_RECURRING_CATEGORY_NAME = "category_name"
        private const val COLUMN_RECURRING_CATEGORY_PATH = "category_path"
        private const val COLUMN_RECURRING_ASSET_ID = "asset_id"
        private const val COLUMN_RECURRING_ASSET_SOURCE = "asset_source"
        private const val COLUMN_RECURRING_DESTINATION_ASSET_ID = "destination_asset_id"
        private const val COLUMN_RECURRING_DESTINATION_ASSET_SOURCE = "destination_asset_source"
        private const val COLUMN_RECURRING_NOTE = "note"
        private const val COLUMN_RECURRING_FREQUENCY = "frequency"
        private const val COLUMN_RECURRING_WEEKLY_DAY = "weekly_day"
        private const val COLUMN_RECURRING_MONTHLY_DAY = "monthly_day"
        private const val COLUMN_RECURRING_YEARLY_MONTH = "yearly_month"
        private const val COLUMN_RECURRING_YEARLY_DAY = "yearly_day"
        private const val COLUMN_RECURRING_INTERVAL_DAYS = "interval_days"
        private const val COLUMN_RECURRING_START_DATE = "start_date"
        private const val COLUMN_RECURRING_END_DATE = "end_date"
        private const val COLUMN_RECURRING_ENABLED = "enabled"
        private const val COLUMN_RECURRING_NEXT_DUE_DATE = "next_due_date"
        private val RECURRING_RECORD_COLUMNS = RecurringRecordRepository.Columns(
            table = TABLE_RECURRING_RECORDS,
            id = COLUMN_RECURRING_ID,
            ledgerId = COLUMN_LEDGER_ID,
            type = COLUMN_RECURRING_TYPE,
            name = COLUMN_RECURRING_NAME,
            amount = COLUMN_RECURRING_AMOUNT,
            categoryId = COLUMN_RECURRING_CATEGORY_ID,
            categoryName = COLUMN_RECURRING_CATEGORY_NAME,
            categoryPath = COLUMN_RECURRING_CATEGORY_PATH,
            assetId = COLUMN_RECURRING_ASSET_ID,
            assetSource = COLUMN_RECURRING_ASSET_SOURCE,
            destinationAssetId = COLUMN_RECURRING_DESTINATION_ASSET_ID,
            destinationAssetSource = COLUMN_RECURRING_DESTINATION_ASSET_SOURCE,
            note = COLUMN_RECURRING_NOTE,
            frequency = COLUMN_RECURRING_FREQUENCY,
            weeklyDay = COLUMN_RECURRING_WEEKLY_DAY,
            monthlyDay = COLUMN_RECURRING_MONTHLY_DAY,
            yearlyMonth = COLUMN_RECURRING_YEARLY_MONTH,
            yearlyDay = COLUMN_RECURRING_YEARLY_DAY,
            intervalDays = COLUMN_RECURRING_INTERVAL_DAYS,
            startDate = COLUMN_RECURRING_START_DATE,
            endDate = COLUMN_RECURRING_END_DATE,
            enabled = COLUMN_RECURRING_ENABLED,
            nextDueDate = COLUMN_RECURRING_NEXT_DUE_DATE
        )

        private const val TABLE_LEDGERS = "ledgers"
        private const val COLUMN_LEDGER_NAME = "name"
        private const val COLUMN_LEDGER_SUBTITLE = "subtitle"
        private const val COLUMN_LEDGER_SORT_ORDER = "sort_order"
        private const val COLUMN_LEDGER_ICON_NAME = "icon_name"
        private const val COLUMN_LEDGER_SHARED_ASSET_IDS = "shared_asset_ids"
        private const val TABLE_LEDGER_SHARED_ASSETS = "ledger_shared_assets"
        private const val COLUMN_SHARED_LEDGER_ID = "ledger_id"
        private const val COLUMN_SHARED_ASSET_ID = "asset_id"
        private const val TABLE_LEDGER_SHARED_LEDGERS = "ledger_shared_ledgers"
        private const val COLUMN_SHARED_SOURCE_LEDGER_ID = "source_ledger_id"

        private const val TABLE_RECORD_DELETION_UNDO = "record_deletion_undo"
        private const val COLUMN_UNDO_TOKEN = "undo_token"
        private const val COLUMN_UNDO_EXPIRES_AT = "expires_at"
        private const val COLUMN_UNDO_BALANCE_DELTA = "balance_delta"

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"
        private const val COLUMN_CATEGORY_TYPE = "type"
        private const val COLUMN_CATEGORY_ICON = "icon"
        private const val COLUMN_CATEGORY_PARENT_ID = "parent_id"
        private const val COLUMN_CATEGORY_SORT_ORDER = "sort_order"
        private const val COLUMN_CATEGORY_COLOR = "color"
        private val CATEGORY_READ_COLUMNS = CategoryReadRepository.Columns(
            table = TABLE_CATEGORIES,
            id = COLUMN_CATEGORY_ID,
            name = COLUMN_CATEGORY_NAME,
            type = COLUMN_CATEGORY_TYPE,
            icon = COLUMN_CATEGORY_ICON,
            color = COLUMN_CATEGORY_COLOR,
            parentId = COLUMN_CATEGORY_PARENT_ID,
            sortOrder = COLUMN_CATEGORY_SORT_ORDER
        )
        private val CATEGORY_WRITE_COLUMNS = CategoryWriteRepository.Columns(
            table = TABLE_CATEGORIES,
            id = COLUMN_CATEGORY_ID,
            name = COLUMN_CATEGORY_NAME,
            type = COLUMN_CATEGORY_TYPE,
            icon = COLUMN_CATEGORY_ICON,
            color = COLUMN_CATEGORY_COLOR,
            parentId = COLUMN_CATEGORY_PARENT_ID,
            sortOrder = COLUMN_CATEGORY_SORT_ORDER,
            ledgerId = COLUMN_LEDGER_ID
        )
        private val CATEGORY_DEFAULTS_COLUMNS = CategoryDefaultsSeeder.Columns(
            table = TABLE_CATEGORIES,
            id = COLUMN_CATEGORY_ID,
            name = COLUMN_CATEGORY_NAME,
            type = COLUMN_CATEGORY_TYPE,
            icon = COLUMN_CATEGORY_ICON,
            parentId = COLUMN_CATEGORY_PARENT_ID,
            sortOrder = COLUMN_CATEGORY_SORT_ORDER,
            ledgerId = COLUMN_LEDGER_ID
        )
        private val LEDGER_READ_COLUMNS = LedgerReadRepository.Columns(
            ledgersTable = TABLE_LEDGERS,
            ledgerId = COLUMN_ID,
            ledgerName = COLUMN_LEDGER_NAME,
            ledgerSubtitle = COLUMN_LEDGER_SUBTITLE,
            ledgerSortOrder = COLUMN_LEDGER_SORT_ORDER,
            ledgerIcon = COLUMN_LEDGER_ICON_NAME,
            sharedLedgersTable = TABLE_LEDGER_SHARED_LEDGERS,
            sharedLedgerId = COLUMN_SHARED_LEDGER_ID,
            sharedSourceLedgerId = COLUMN_SHARED_SOURCE_LEDGER_ID,
            sharedAssetsTable = TABLE_LEDGER_SHARED_ASSETS,
            sharedAssetId = COLUMN_SHARED_ASSET_ID,
            assetsTable = TABLE_ASSETS,
            assetId = COLUMN_ASSET_ID,
            assetLedgerId = COLUMN_LEDGER_ID,
            assetAmount = COLUMN_ASSET_AMOUNT,
            assetArchived = COLUMN_ASSET_IS_ARCHIVED,
            assetIncludeInTotal = COLUMN_ASSET_INCLUDE_IN_TOTAL,
            recordsTable = TABLE_RECORDS,
            recordId = COLUMN_ID,
            recordLedgerId = COLUMN_LEDGER_ID
        )
        private val LEDGER_WRITE_COLUMNS = LedgerWriteRepository.Columns(
            ledgersTable = TABLE_LEDGERS,
            ledgerId = COLUMN_ID,
            ledgerName = COLUMN_LEDGER_NAME,
            ledgerSubtitle = COLUMN_LEDGER_SUBTITLE,
            ledgerSortOrder = COLUMN_LEDGER_SORT_ORDER,
            ledgerIcon = COLUMN_LEDGER_ICON_NAME,
            sharedLedgersTable = TABLE_LEDGER_SHARED_LEDGERS,
            sharedLedgerId = COLUMN_SHARED_LEDGER_ID,
            sharedSourceLedgerId = COLUMN_SHARED_SOURCE_LEDGER_ID,
            sharedAssetsTable = TABLE_LEDGER_SHARED_ASSETS,
            sharedAssetId = COLUMN_SHARED_ASSET_ID,
            assetsTable = TABLE_ASSETS,
            assetId = COLUMN_ASSET_ID,
            assetLedgerId = COLUMN_LEDGER_ID,
            assetName = COLUMN_ASSET_NAME,
            assetAmount = COLUMN_ASSET_AMOUNT,
            assetType = COLUMN_ASSET_TYPE,
            assetCategoryLabel = COLUMN_ASSET_CATEGORY_LABEL,
            assetCategoryIcon = COLUMN_ASSET_CATEGORY_ICON_NAME,
            assetArchived = COLUMN_ASSET_IS_ARCHIVED,
            assetPinned = COLUMN_ASSET_IS_PINNED,
            assetIncludeInTotal = COLUMN_ASSET_INCLUDE_IN_TOTAL,
            recordsTable = TABLE_RECORDS,
            recordLedgerId = COLUMN_LEDGER_ID
        )

        private const val TABLE_ASSETS = "assets"
        private const val COLUMN_ASSET_ID = "id"
        private const val COLUMN_ASSET_NAME = "name"
        private const val COLUMN_ASSET_AMOUNT = "amount"
        private const val COLUMN_ASSET_TYPE = "type"
        private const val COLUMN_ASSET_CATEGORY_LABEL = "category_label"
        private const val COLUMN_ASSET_CATEGORY_ICON_NAME = "category_icon_name"
        private const val COLUMN_ASSET_IS_ARCHIVED = "is_archived"
        private const val COLUMN_ASSET_IS_PINNED = "is_pinned"
        private const val COLUMN_ASSET_INCLUDE_IN_TOTAL = "include_in_total"
        private const val COLUMN_ASSET_SORT_ORDER = "sort_order"
        private val ASSET_READ_COLUMNS = AssetReadRepository.Columns(
            table = TABLE_ASSETS,
            id = COLUMN_ASSET_ID,
            ledgerId = COLUMN_LEDGER_ID,
            name = COLUMN_ASSET_NAME,
            amount = COLUMN_ASSET_AMOUNT,
            type = COLUMN_ASSET_TYPE,
            categoryLabel = COLUMN_ASSET_CATEGORY_LABEL,
            categoryIcon = COLUMN_ASSET_CATEGORY_ICON_NAME,
            archived = COLUMN_ASSET_IS_ARCHIVED,
            pinned = COLUMN_ASSET_IS_PINNED,
            includeInTotal = COLUMN_ASSET_INCLUDE_IN_TOTAL,
            sortOrder = COLUMN_ASSET_SORT_ORDER
        )
        private val ASSET_WRITE_COLUMNS = AssetWriteRepository.Columns(
            table = TABLE_ASSETS,
            id = COLUMN_ASSET_ID,
            ledgerId = COLUMN_LEDGER_ID,
            name = COLUMN_ASSET_NAME,
            amount = COLUMN_ASSET_AMOUNT,
            type = COLUMN_ASSET_TYPE,
            categoryLabel = COLUMN_ASSET_CATEGORY_LABEL,
            categoryIcon = COLUMN_ASSET_CATEGORY_ICON_NAME,
            archived = COLUMN_ASSET_IS_ARCHIVED,
            pinned = COLUMN_ASSET_IS_PINNED,
            includeInTotal = COLUMN_ASSET_INCLUDE_IN_TOTAL,
            sortOrder = COLUMN_ASSET_SORT_ORDER
        )
        private val RECORD_ASSET_BALANCE_COLUMNS = RecordAssetBalanceRepository.Columns(
            assetsTable = TABLE_ASSETS,
            assetId = COLUMN_ASSET_ID,
            assetName = COLUMN_ASSET_NAME,
            assetAmount = COLUMN_ASSET_AMOUNT,
            assetArchived = COLUMN_ASSET_IS_ARCHIVED
        )

        private const val TABLE_AI_CHAT_SESSIONS = "ai_chat_sessions"
        private const val COLUMN_AI_CHAT_SESSION_ID = "id"
        private const val COLUMN_AI_CHAT_SESSION_TITLE = "title"
        private const val COLUMN_AI_CHAT_SESSION_CREATED_AT = "created_at"
        private const val COLUMN_AI_CHAT_SESSION_UPDATED_AT = "updated_at"

        private const val TABLE_AI_CHAT_MESSAGES = "ai_chat_messages"
        private const val COLUMN_AI_CHAT_MESSAGE_ID = "id"
        private const val COLUMN_AI_CHAT_MESSAGE_SESSION_ID = "session_id"
        private const val COLUMN_AI_CHAT_MESSAGE_ROLE = "role"
        private const val COLUMN_AI_CHAT_MESSAGE_CONTENT = "content"
        private const val COLUMN_AI_CHAT_MESSAGE_REASONING = "reasoning_content"
        private const val COLUMN_AI_CHAT_MESSAGE_IS_ERROR = "is_error"
        private const val COLUMN_AI_CHAT_MESSAGE_CREATED_AT = "created_at"
        private val AI_CHAT_COLUMNS = AiChatRepository.Columns(
            sessionsTable = TABLE_AI_CHAT_SESSIONS,
            sessionId = COLUMN_AI_CHAT_SESSION_ID,
            sessionTitle = COLUMN_AI_CHAT_SESSION_TITLE,
            sessionCreatedAt = COLUMN_AI_CHAT_SESSION_CREATED_AT,
            sessionUpdatedAt = COLUMN_AI_CHAT_SESSION_UPDATED_AT,
            messagesTable = TABLE_AI_CHAT_MESSAGES,
            messageId = COLUMN_AI_CHAT_MESSAGE_ID,
            messageSessionId = COLUMN_AI_CHAT_MESSAGE_SESSION_ID,
            messageRole = COLUMN_AI_CHAT_MESSAGE_ROLE,
            messageContent = COLUMN_AI_CHAT_MESSAGE_CONTENT,
            messageReasoning = COLUMN_AI_CHAT_MESSAGE_REASONING,
            messageIsError = COLUMN_AI_CHAT_MESSAGE_IS_ERROR,
            messageCreatedAt = COLUMN_AI_CHAT_MESSAGE_CREATED_AT
        )
        private val RECORD_STATISTICS_COLUMNS = RecordStatisticsRepository.Columns(
            table = TABLE_RECORDS,
            ledgerId = COLUMN_LEDGER_ID,
            date = COLUMN_DATE,
            amount = COLUMN_AMOUNT,
            category = COLUMN_CATEGORY,
            type = COLUMN_TYPE,
            fee = COLUMN_RECORD_FEE
        )

        private const val CREATE_TABLE_RECORDS =
            "CREATE TABLE $TABLE_RECORDS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT INTEGER NOT NULL CHECK ($COLUMN_AMOUNT > 0), " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL CHECK ($COLUMN_TYPE IN (0, 1, 2)), " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_FEE INTEGER NOT NULL DEFAULT 0 CHECK ($COLUMN_RECORD_FEE >= 0), " +
            "$COLUMN_RECORD_ASSET_ID INTEGER, " +
            "$COLUMN_RECORD_DESTINATION_ASSET_ID INTEGER, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_DESTINATION_ASSET_SOURCE TEXT, " +
            "$COLUMN_PHOTO_URI TEXT, " +
            "$COLUMN_PHOTO_URIS TEXT, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "CHECK ($COLUMN_TYPE != 2 OR ($COLUMN_RECORD_ASSET_ID IS NOT NULL AND $COLUMN_RECORD_DESTINATION_ASSET_ID IS NOT NULL AND $COLUMN_RECORD_ASSET_ID != $COLUMN_RECORD_DESTINATION_ASSET_ID)))"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY_ICON TEXT, " +
            "$COLUMN_CATEGORY_COLOR TEXT NOT NULL DEFAULT '#F5F5F5', " +
            "$COLUMN_CATEGORY_PARENT_ID INTEGER, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_CATEGORY_SORT_ORDER INTEGER NOT NULL DEFAULT 0)"

        private const val CREATE_TABLE_ASSETS =
            "CREATE TABLE $TABLE_ASSETS (" +
            "$COLUMN_ASSET_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_ASSET_NAME TEXT NOT NULL, " +
            "$COLUMN_ASSET_AMOUNT INTEGER NOT NULL, " +
            "$COLUMN_ASSET_TYPE INTEGER NOT NULL, " +
            "$COLUMN_ASSET_CATEGORY_LABEL TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_CATEGORY_ICON_NAME TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_IS_PINNED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_INCLUDE_IN_TOTAL INTEGER DEFAULT 1, " +
            "$COLUMN_ASSET_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL)"

        private const val CREATE_TABLE_RECORD_DELETION_UNDO =
            "CREATE TABLE $TABLE_RECORD_DELETION_UNDO (" +
            "$COLUMN_UNDO_TOKEN TEXT PRIMARY KEY, " +
            "$COLUMN_UNDO_EXPIRES_AT INTEGER NOT NULL, " +
            "$COLUMN_UNDO_BALANCE_DELTA INTEGER NOT NULL, " +
            "$COLUMN_ID INTEGER NOT NULL, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_FEE INTEGER NOT NULL DEFAULT 0, " +
            "$COLUMN_RECORD_ASSET_ID INTEGER, " +
            "$COLUMN_RECORD_DESTINATION_ASSET_ID INTEGER, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_DESTINATION_ASSET_SOURCE TEXT, " +
            "$COLUMN_PHOTO_URI TEXT, " +
            "$COLUMN_PHOTO_URIS TEXT, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_SORT_ORDER INTEGER NOT NULL)"

        private const val CREATE_TABLE_LEDGERS =
            "CREATE TABLE $TABLE_LEDGERS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_LEDGER_NAME TEXT NOT NULL, " +
            "$COLUMN_LEDGER_SUBTITLE TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_LEDGER_SHARED_ASSET_IDS TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_LEDGER_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "$COLUMN_LEDGER_ICON_NAME TEXT NOT NULL DEFAULT 'tabler_book')"

        private const val CREATE_TABLE_LEDGER_SHARED_ASSETS =
            "CREATE TABLE $TABLE_LEDGER_SHARED_ASSETS (" +
            "$COLUMN_SHARED_LEDGER_ID INTEGER NOT NULL, " +
            "$COLUMN_SHARED_ASSET_ID INTEGER NOT NULL, " +
            "PRIMARY KEY ($COLUMN_SHARED_LEDGER_ID, $COLUMN_SHARED_ASSET_ID))"

        private const val CREATE_TABLE_LEDGER_SHARED_LEDGERS =
            "CREATE TABLE $TABLE_LEDGER_SHARED_LEDGERS (" +
            "$COLUMN_SHARED_LEDGER_ID INTEGER PRIMARY KEY, " +
            "$COLUMN_SHARED_SOURCE_LEDGER_ID INTEGER NOT NULL)"

        private const val CREATE_TABLE_AI_CHAT_SESSIONS =
            "CREATE TABLE $TABLE_AI_CHAT_SESSIONS (" +
            "$COLUMN_AI_CHAT_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_AI_CHAT_SESSION_TITLE TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_SESSION_CREATED_AT INTEGER NOT NULL, " +
            "$COLUMN_AI_CHAT_SESSION_UPDATED_AT INTEGER NOT NULL)"

        private const val CREATE_TABLE_AI_CHAT_MESSAGES =
            "CREATE TABLE $TABLE_AI_CHAT_MESSAGES (" +
            "$COLUMN_AI_CHAT_MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_AI_CHAT_MESSAGE_SESSION_ID INTEGER NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_ROLE TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_CONTENT TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_REASONING TEXT, " +
            "$COLUMN_AI_CHAT_MESSAGE_IS_ERROR INTEGER DEFAULT 0, " +
            "$COLUMN_AI_CHAT_MESSAGE_CREATED_AT INTEGER NOT NULL)"

        private const val CREATE_TABLE_RECURRING_RECORDS =
            "CREATE TABLE $TABLE_RECURRING_RECORDS (" +
                "$COLUMN_RECURRING_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_LEDGER_ID INTEGER NOT NULL, " +
                 "$COLUMN_RECURRING_TYPE INTEGER NOT NULL CHECK ($COLUMN_RECURRING_TYPE IN (0, 1, 2)), " +
                "$COLUMN_RECURRING_NAME TEXT NOT NULL, " +
                "$COLUMN_RECURRING_AMOUNT INTEGER NOT NULL CHECK ($COLUMN_RECURRING_AMOUNT > 0), " +
                "$COLUMN_RECURRING_CATEGORY_ID INTEGER, " +
                "$COLUMN_RECURRING_CATEGORY_NAME TEXT NOT NULL, " +
                "$COLUMN_RECURRING_CATEGORY_PATH TEXT, " +
                "$COLUMN_RECURRING_ASSET_ID INTEGER, " +
                 "$COLUMN_RECURRING_ASSET_SOURCE TEXT, " +
                 "$COLUMN_RECURRING_DESTINATION_ASSET_ID INTEGER, " +
                 "$COLUMN_RECURRING_DESTINATION_ASSET_SOURCE TEXT, " +
                "$COLUMN_RECURRING_NOTE TEXT, " +
                 "$COLUMN_RECURRING_FREQUENCY TEXT NOT NULL, " +
                 "$COLUMN_RECURRING_WEEKLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_MONTHLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_YEARLY_MONTH INTEGER, " +
                 "$COLUMN_RECURRING_YEARLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_INTERVAL_DAYS INTEGER, " +
                "$COLUMN_RECURRING_START_DATE TEXT NOT NULL, " +
                "$COLUMN_RECURRING_END_DATE TEXT, " +
                "$COLUMN_RECURRING_ENABLED INTEGER NOT NULL DEFAULT 1, " +
                "$COLUMN_RECURRING_NEXT_DUE_DATE TEXT NOT NULL)"

        private const val CREATE_INDEX_RECURRING_DUE =
            "CREATE INDEX IF NOT EXISTS idx_recurring_ledger_due ON $TABLE_RECURRING_RECORDS($COLUMN_LEDGER_ID, $COLUMN_RECURRING_ENABLED, $COLUMN_RECURRING_NEXT_DUE_DATE)"

        private const val CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at ON $TABLE_AI_CHAT_SESSIONS($COLUMN_AI_CHAT_SESSION_UPDATED_AT DESC)"

        private const val CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_created_at ON $TABLE_AI_CHAT_MESSAGES($COLUMN_AI_CHAT_MESSAGE_SESSION_ID, $COLUMN_AI_CHAT_MESSAGE_CREATED_AT ASC)"

        private const val CREATE_INDEX_RECORDS_LEDGER_DATE =
            "CREATE INDEX IF NOT EXISTS idx_records_ledger_date_sort ON $TABLE_RECORDS($COLUMN_LEDGER_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE =
            "CREATE INDEX IF NOT EXISTS idx_records_ledger_type_date ON $TABLE_RECORDS($COLUMN_LEDGER_ID, $COLUMN_TYPE, $COLUMN_DATE DESC)"
        private const val CREATE_INDEX_RECORDS_SOURCE_ASSET =
            "CREATE INDEX IF NOT EXISTS idx_records_source_asset_date ON $TABLE_RECORDS($COLUMN_RECORD_ASSET_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_RECORDS_DESTINATION_ASSET =
            "CREATE INDEX IF NOT EXISTS idx_records_destination_asset_date ON $TABLE_RECORDS($COLUMN_RECORD_DESTINATION_ASSET_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_CATEGORIES_TYPE_PARENT =
            "CREATE INDEX IF NOT EXISTS idx_categories_type_parent_sort ON $TABLE_CATEGORIES($COLUMN_CATEGORY_TYPE, $COLUMN_CATEGORY_PARENT_ID, $COLUMN_CATEGORY_SORT_ORDER, $COLUMN_CATEGORY_ID)"
        private const val CREATE_INDEX_ASSETS_LEDGER_ARCHIVED =
            "CREATE INDEX IF NOT EXISTS idx_assets_ledger_archived_name ON $TABLE_ASSETS($COLUMN_LEDGER_ID, $COLUMN_ASSET_IS_ARCHIVED, $COLUMN_ASSET_NAME)"
        private const val CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER =
            "CREATE INDEX IF NOT EXISTS idx_assets_ledger_archived_order ON $TABLE_ASSETS($COLUMN_LEDGER_ID, $COLUMN_ASSET_IS_ARCHIVED, $COLUMN_ASSET_SORT_ORDER, $COLUMN_ASSET_ID)"
    }

    data class RecordPageCursor(
        val sortOrder: Int,
        val recordId: Long
    )

    data class RecordPage(
        val records: List<Record>,
        val nextCursor: RecordPageCursor?
    )

    data class RecordDeletionToken(
        val value: String,
        val expiresAtEpochMs: Long
    )

    private fun currentLedgerId(): Long {
        val saved = LedgerSession.getCurrentId(appContext)
        if (saved != null && ledgerReadRepository.containsLedger(saved)) return saved
        val id = ledgerReadRepository.getMasterLedgerId()
        LedgerSession.setCurrentId(appContext, id)
        return id
    }

    private fun assetPoolRootId(ledgerId: Long): Long {
        val visited = mutableSetOf<Long>()
        var root = ledgerId
        while (visited.add(root)) root = getSharedSourceLedgerId(root) ?: break
        return root
    }

    /** Assets owned by the active ledger's effective asset pool. */
    private fun assetScope(): String {
        return "$COLUMN_LEDGER_ID = ${assetPoolRootId(currentLedgerId())}"
    }

    fun getLedgers(): List<com.example.cardtally.model.Ledger> = ledgerReadRepository.getLedgers()

    /** The first ledger owns the permanent master asset pool. */
    fun getMasterLedgerId(): Long = ledgerReadRepository.getMasterLedgerId()

    fun addLedger(name: String, subtitle: String = "", iconName: String = "tabler_book"): Long =
        ledgerWriteRepository.addLedger(name, subtitle, iconName)

    fun addLedger(name: String, subtitle: String = "", sharedAssetIds: List<Long>): Long =
        ledgerWriteRepository.addLedgerWithAssetLinks(name, subtitle, sharedAssetIds, emptyList())

    fun addLedgerWithAssetPool(
        name: String,
        sharedSourceLedgerId: Long? = null,
        copyFromLedgerId: Long? = null,
        iconName: String = "tabler_book"
    ): Long = ledgerWriteRepository.addLedgerWithAssetPool(name, sharedSourceLedgerId, copyFromLedgerId, iconName)

    fun getSharedSourceLedgerId(ledgerId: Long): Long? =
        ledgerReadRepository.getSharedSourceLedgerId(ledgerId)

    /** Copies a shared pool to one ledger and normalizes a one-ledger remainder to independent pools. */
    fun forkLedgerAssetPool(ledgerId: Long): Boolean = ledgerWriteRepository.forkAssetPool(ledgerId)

    fun getLedgerAssetPoolSummary(ledgerId: Long): Pair<Int, Double> =
        ledgerReadRepository.getAssetPoolSummary(assetPoolRootId(ledgerId))

    fun updateLedgerAssetPool(ledgerId: Long, sharedSourceLedgerId: Long?, iconName: String? = null, name: String? = null): Boolean =
        ledgerWriteRepository.updateAssetPool(ledgerId, sharedSourceLedgerId, iconName, name)

    fun updateLedgerDetails(ledgerId: Long, iconName: String? = null, name: String? = null): Boolean =
        ledgerWriteRepository.updateLedgerDetails(ledgerId, iconName, name)

    fun addLedger(name: String, subtitle: String = "", sharedAssetIds: List<Long>, copiedAssetIds: List<Long>): Long =
        ledgerWriteRepository.addLedgerWithAssetLinks(name, subtitle, sharedAssetIds, copiedAssetIds)

    fun copyAssetToLedger(assetId: Long, targetLedgerId: Long): Long =
        ledgerWriteRepository.copyAssetToLedger(assetId, targetLedgerId)

    fun updateLedger(ledgerId: Long, name: String, sharedAssetIds: List<Long>): Boolean =
        ledgerWriteRepository.updateLegacyLedger(ledgerId, name, sharedAssetIds)

    fun getSharedAssetIds(ledgerId: Long): Set<Long> = ledgerReadRepository.getSharedAssetIds(ledgerId)

    fun canManageAsset(assetId: Long): Boolean = ledgerReadRepository.canManageAsset(assetId, currentLedgerId())

    fun getLedgerRecordCount(ledgerId: Long): Int = ledgerReadRepository.getLedgerRecordCount(ledgerId)

    /** Deletes ledgers and their records while retaining a shared pool for surviving ledgers. */
    fun deleteLedgers(ledgerIds: Set<Long>): Boolean = ledgerWriteRepository.deleteLedgers(ledgerIds)

    fun getCurrentLedger(): com.example.cardtally.model.Ledger? = getLedgers().firstOrNull { it.id == currentLedgerId() }

    override fun onCreate(db: SQLiteDatabase) = schemaCreator.create(db)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        schemaUpgradeManager.upgrade(db, oldVersion)
    }

    private fun resetAllDataForVerification(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORD_DELETION_UNDO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_LEDGERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AI_CHAT_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AI_CHAT_SESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECURRING_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGERS")
        onCreate(db)
        LedgerSession.setCurrentId(appContext, 1L)
    }

    private fun createPerformanceIndices(db: SQLiteDatabase) {
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_SOURCE_ASSET)
        db.execSQL(CREATE_INDEX_RECORDS_DESTINATION_ASSET)
        db.execSQL(CREATE_INDEX_CATEGORIES_TYPE_PARENT)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER)
        db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
        db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        db.execSQL(CREATE_INDEX_RECURRING_DUE)
    }

    private fun ensureColumn(
        db: SQLiteDatabase,
        tableName: String,
        columnName: String,
        alterStatement: String
    ) {
        if (!tableHasColumn(db, tableName, columnName)) {
            db.execSQL(alterStatement)
        }
    }

    private fun tableHasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        cursor.use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    private fun createRecordValues(record: Record): ContentValues =
        RecordSqlMapper.toContentValues(record, currentLedgerId(), RECORD_SQL_COLUMNS)

    private fun createRecordFromCursor(cursor: Cursor): Record =
        RecordSqlMapper.fromCursor(cursor, RECORD_SQL_COLUMNS)

    private fun getNullableString(cursor: Cursor, columnName: String): String? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return null
        }
        return cursor.getString(columnIndex)
    }

    private fun getNullableLong(cursor: Cursor, columnName: String): Long? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return null
        }
        return cursor.getLong(columnIndex)
    }

    private fun getNullableInt(cursor: Cursor, columnName: String): Int? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) return null
        return cursor.getInt(columnIndex)
    }

    private fun getCategoryByIdInternal(db: SQLiteDatabase, id: Long): Category? =
        categoryReadRepository.getById(db, id)

    private fun getCategoriesByTypeInternal(db: SQLiteDatabase, type: Int): List<Category> =
        categoryReadRepository.getByType(db, type)

    private fun validateParentAssignment(db: SQLiteDatabase, category: Category) {
        categoryHierarchyValidator.validateParentAssignment(db, category)
    }

    private fun categoryHasChildren(db: SQLiteDatabase, id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_PARENT_ID = ?",
            arrayOf(id.toString())
        )

        cursor.use {
            return it.moveToFirst() && it.getInt(0) > 0
        }
    }

    private fun categoryIsReferencedByRecordId(db: SQLiteDatabase, id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT 1 FROM (" +
                "SELECT $COLUMN_RECORD_CATEGORY_ID FROM $TABLE_RECORDS " +
                "WHERE $COLUMN_RECORD_CATEGORY_ID = ? " +
                "UNION ALL " +
                "SELECT $COLUMN_RECORD_CATEGORY_ID FROM $TABLE_RECORD_DELETION_UNDO " +
                "WHERE $COLUMN_RECORD_CATEGORY_ID = ? AND $COLUMN_UNDO_EXPIRES_AT > ?" +
                ") LIMIT 1",
            arrayOf(id.toString(), id.toString(), System.currentTimeMillis().toString())
        )

        cursor.use {
            return it.moveToFirst()
        }
    }

    fun addRecord(record: Record): Long = recordWriteRepository.add(record)

    fun getAllRecords(): List<Record> = recordReadRepository.getAll()

    fun updateRecord(record: Record): Int = recordWriteRepository.update(record)

    fun getRecurringRecords(): List<RecurringRecord> = recurringRepository.getForCurrentLedger()

    fun getAllRecurringRecords(): List<RecurringRecord> = recurringRepository.getAll()

    fun getRecurringRecord(id: Long): RecurringRecord? = recurringRepository.getForCurrentLedger(id)

    fun getRecurringRecordById(id: Long): RecurringRecord? = recurringRepository.getById(id)

    fun saveRecurringRecord(recurring: RecurringRecord): Long = recurringRepository.save(recurring)

    fun setRecurringEnabled(id: Long, enabled: Boolean): Boolean = recurringRepository.setEnabled(id, enabled)

    fun deleteRecurringRecord(id: Long): Boolean = recurringRepository.delete(id)

    fun getEarliestRecurringDueDate(): String? = recurringRepository.getEarliestDueDate()

    /** Repairs stale due dates left behind when a recurring rule was changed. */
    fun repairInvalidRecurringNextDueDates(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            val selectedLedgerId = LedgerSession.getCurrentId(appContext)
            var repaired = 0
            getLedgers().forEach { ledger ->
                LedgerSession.setCurrentId(appContext, ledger.id)
                getRecurringRecords().forEach { template ->
                    val normalized = RecurringScheduleCalculator.normalizeDueDate(template.nextDueDate, template)
                    if (normalized != template.nextDueDate) {
                        recurringRepository.updateNextDueDate(template.id, ledger.id, normalized)
                        repaired++
                    }
                }
            }
            selectedLedgerId?.let { LedgerSession.setCurrentId(appContext, it) }
            repaired
        }

    /** Processes every ledger without changing which ledger the user has selected. */
    fun processDueRecurringRecordsForAllLedgers(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            val selectedLedgerId = LedgerSession.getCurrentId(appContext)
            var generated = 0
            getLedgers().forEach { ledger ->
                LedgerSession.setCurrentId(appContext, ledger.id)
                generated += processDueRecurringRecordsForCurrentLedger(today)
            }
            selectedLedgerId?.let { LedgerSession.setCurrentId(appContext, it) }
            generated
        }

    /** Generates all missed occurrences for the active ledger when the app returns to the foreground. */
    fun processDueRecurringRecords(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            processDueRecurringRecordsForCurrentLedger(today)
        }

    private fun processDueRecurringRecordsForCurrentLedger(today: String): Int {
        var generated = 0
        getRecurringRecords().filter { it.enabled }.forEach { template ->
            var due = RecurringScheduleCalculator.normalizeDueDate(template.nextDueDate, template)
            while (due <= today && (template.endDate == null || due <= template.endDate!!)) {
                val recordId = addRecord(Record(
                    date = due,
                    amount = Money.toMajorDouble(template.amountMinor),
                    category = template.categoryName,
                    categoryId = template.categoryId,
                    categoryNameSnapshot = template.categoryName,
                    categoryPathSnapshot = template.categoryPath,
                    type = template.type,
                    description = template.note,
                    assetId = template.assetId,
                    assetSource = template.assetSource,
                    destinationAssetId = template.destinationAssetId,
                    destinationAssetSource = template.destinationAssetSource,
                    ledgerId = template.ledgerId
                ))
                if (recordId <= 0L) break
                generated++
                due = RecurringScheduleCalculator.nextDate(due, template)
            }
            val stillActive = template.endDate == null || due <= template.endDate!!
            recurringRepository.updateNextDueDate(
                template.id,
                currentLedgerId(),
                due,
                enabled = if (stillActive) null else false
            )
        }
        return generated
    }

    private fun validateRecordForWrite(db: SQLiteDatabase, record: Record) {
        recordWriteValidator.validate(db, record)
    }

    fun getRecordById(id: Long): Record? {
        val db = readableDatabase
        val record = getRecordByIdInternal(db, id)
        return record
    }

    private fun getRecordByIdInternal(db: SQLiteDatabase, id: Long): Record? =
        recordReadRepository.getById(db, id)

    fun deleteRecord(
        id: Long,
        deletedAtEpochMs: Long = System.currentTimeMillis()
    ): RecordDeletionToken? = recordWriteRepository.delete(id, deletedAtEpochMs, RECORD_UNDO_WINDOW_MS)

    private fun findCategoryId(
        db: SQLiteDatabase,
        name: String,
        type: Int,
        parentId: Long?,
        ledgerId: Long = 1L
    ): Long? {
        val selection = if (parentId == null) {
            "$COLUMN_CATEGORY_NAME = ? AND $COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID IS NULL"
        } else {
            "$COLUMN_CATEGORY_NAME = ? AND $COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID = ?"
        }
        val args = if (parentId == null) {
            arrayOf(name, type.toString())
        } else {
            arrayOf(name, type.toString(), parentId.toString())
        }
        return db.query(
            TABLE_CATEGORIES,
            arrayOf(COLUMN_CATEGORY_ID),
            selection,
            args,
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    fun undoRecordDeletion(
        token: RecordDeletionToken,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean = recordWriteRepository.undoDeletion(token, nowEpochMs)

    private fun createRecordDeletionUndoValues(
        record: Record,
        token: RecordDeletionToken,
        balanceDelta: Double
    ): ContentValues {
        return ContentValues().apply {
            put(COLUMN_LEDGER_ID, currentLedgerId())
            put(COLUMN_UNDO_TOKEN, token.value)
            put(COLUMN_UNDO_EXPIRES_AT, token.expiresAtEpochMs)
            put(COLUMN_UNDO_BALANCE_DELTA, requireNotNull(Money.toMinor(balanceDelta)))
            put(COLUMN_ID, record.id)
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, requireNotNull(Money.toMinor(record.amount)))
            put(COLUMN_CATEGORY, record.category)
            putNullable(COLUMN_RECORD_CATEGORY_ID, record.categoryId)
            putNullable(COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT, record.categoryNameSnapshot)
            putNullable(COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT, record.categoryPathSnapshot)
            put(COLUMN_TYPE, record.type)
            putNullable(COLUMN_DESCRIPTION, record.description)
            put(COLUMN_RECORD_FEE, requireNotNull(Money.toMinor(record.fee)))
            putNullable(COLUMN_RECORD_ASSET_ID, record.assetId)
            putNullable(COLUMN_RECORD_DESTINATION_ASSET_ID, record.destinationAssetId)
            putNullable(COLUMN_ASSET_SOURCE, record.assetSource)
            putNullable(COLUMN_PHOTO_URI, record.photoUri)
            putNullable(COLUMN_PHOTO_URIS, record.photoUris.joinToString(PHOTO_URI_SEPARATOR))
            put(COLUMN_SORT_ORDER, record.sortOrder)
        }
    }

    private fun deleteRecordPhotoUris(record: Record) {
        (record.photoUris.ifEmpty { record.photoUri?.let(::listOf) ?: emptyList() }).forEach { photoUri ->
            runCatching {
                appContext.contentResolver.delete(android.net.Uri.parse(photoUri), null, null)
            }
        }
    }

    private fun ContentValues.putNullable(columnName: String, value: String?) {
        if (value == null) putNull(columnName) else put(columnName, value)
    }

    private fun ContentValues.putNullable(columnName: String, value: Long?) {
        if (value == null) putNull(columnName) else put(columnName, value)
    }

    fun updateRecordSortOrder(recordId: Long, newSortOrder: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SORT_ORDER, newSortOrder)
        }
        db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?", arrayOf(recordId.toString()))
    }

    fun updateRecordsSortOrder(records: List<Record>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (record in records) {
                val values = ContentValues().apply {
                    put(COLUMN_SORT_ORDER, record.sortOrder)
                }
                db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?", arrayOf(record.id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCurrentDate(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun addCategory(category: Category): Long = categoryWriteRepository.add(category)

    fun updateCategorySortOrders(categoryIds: List<Long>) =
        categoryWriteRepository.updateSortOrders(categoryIds)

    private fun initializeCategorySortOrders(db: SQLiteDatabase) =
        categoryWriteRepository.initializeSortOrders(db)

    private fun initializeAssetSortOrders(db: SQLiteDatabase) {
        db.rawQuery(
            "SELECT $COLUMN_ASSET_ID FROM $TABLE_ASSETS ORDER BY $COLUMN_ASSET_NAME COLLATE NOCASE, $COLUMN_ASSET_ID",
            null
        ).use { cursor ->
            var order = 0
            while (cursor.moveToNext()) {
                db.update(
                    TABLE_ASSETS,
                    ContentValues().apply { put(COLUMN_ASSET_SORT_ORDER, order++) },
                    "$COLUMN_ASSET_ID = ?",
                    arrayOf(cursor.getLong(0).toString())
                )
            }
        }
    }

    fun getCategoriesByType(type: Int): List<Category> = categoryReadRepository.getByType(type)

    fun getCategoryTreeByType(type: Int): List<Category> {
        val db = readableDatabase
        ensureDefaultCategoriesForCurrentLedger(db)
        val categories = getCategoriesByTypeInternal(db, type)
        db.close()

        return CategoryTreeOrdering.order(categories)
    }

    private fun ensureDefaultCategoriesForCurrentLedger(db: SQLiteDatabase) {
        val hasCategories = db.rawQuery("SELECT 1 FROM $TABLE_CATEGORIES LIMIT 1", null).use { it.moveToFirst() }
        if (!hasCategories) {
            categoryDefaultsSeeder.seed(db, 1L)
        }
    }

    fun getLeafCategoriesByType(type: Int): List<Category> =
        categoryReadRepository.getLeavesByType(type)

    fun buildCategoryPathLabel(categoryId: Long): String? =
        categoryReadRepository.buildPathLabel(categoryId)

    fun getCategoryById(id: Long): Category? =
        categoryReadRepository.getById(readableDatabase, id)

    fun getAllCategories(): List<Category> {
        val db = readableDatabase
        val categories = mutableListOf<Category>()
        categories += getCategoriesByTypeInternal(db, 0)
        categories += getCategoriesByTypeInternal(db, 1)
        db.close()
        return categories
    }

    fun updateCategory(category: Category): Int = categoryWriteRepository.update(category)

    fun deleteCategory(id: Long) = categoryWriteRepository.delete(id)

    fun getTotalByType(type: Int): Double = Money.toMajorDouble(getTotalByTypeMinor(type))

    fun getTotalByTypeMinor(type: Int): Long = recordStatisticsRepository.totalByTypeMinor(type)

    fun getTotalByTypeAndDateRange(type: Int, startDate: String, endDate: String): Double =
        Money.toMajorDouble(getTotalByTypeAndDateRangeMinor(type, startDate, endDate))

    fun getTotalByTypeAndDateRangeMinor(type: Int, startDate: String, endDate: String): Long =
        recordStatisticsRepository.totalByTypeAndDateRangeMinor(type, startDate, endDate)

    fun getRecordsByDateRange(startDate: String, endDate: String): List<Record> =
        recordReadRepository.byDateRange(startDate, endDate)

    fun getTodayRecordsPage(
        todayDate: String = getCurrentDate(),
        after: RecordPageCursor? = null
    ): RecordPage = recordReadRepository.todayPage(todayDate, after)

    fun getRecordsByAssetSource(assetSource: String): List<Record> =
        recordReadRepository.byAssetSource(assetSource)

    fun getRecordsByAssetId(assetId: Long): List<Record> = recordReadRepository.byAssetId(assetId)

    /**
     * Every ledger that references [assetId], in every ledger — the asset-detail
     * history is a property of the asset id, not of the currently selected ledger.
     * Each row appears once even when the same asset is both source and
     * destination (a transfer to itself is still a single record).
     */
    fun getAllRecordsByAssetId(assetId: Long): List<Record> = recordReadRepository.allByAssetId(assetId)

    /** Ledger names keyed by id, for bulk provenance labels (one query, no N+1). */
    fun getLedgerNamesByIds(ledgerIds: Set<Long>): Map<Long, String> =
        ledgerReadRepository.getLedgerNamesByIds(ledgerIds)

    /** A ledger's ledger-row id, needed to flag "this row belongs to this ledger". */
    fun getLedgerIdForRecord(recordId: Long): Long? = ledgerReadRepository.getLedgerIdForRecord(recordId)

    /**
     * One row per distinct asset group (pool root). [rootLedgerId] and
     * [rootLedgerName] describe the owner; [memberLedgerIds] are the ledgers that
     * share the same pool, and [sameNameGroupCount] distinguishes identically
     * named groups without forcing the user to maintain group names.
     */
    data class AssetGroupDescriptor(
        val rootLedgerId: Long,
        val rootLedgerName: String,
        val memberLedgerIds: List<Long>,
        val memberLedgerNames: List<String>,
        val isMaster: Boolean,
        val assetCount: Int,
        val sameNameGroupCount: Int
    )

    /** Stable pool root of a ledger (follows shared-source chains, cycle-safe). */
    fun getAssetGroupRootId(ledgerId: Long): Long = assetPoolRootId(ledgerId)

    /** Ledgers that resolve to the same asset pool as [ledgerId], including itself. */
    fun getAssetGroupMemberIds(ledgerId: Long): List<Long> {
        val root = assetPoolRootId(ledgerId)
        return getLedgers().map { it.id }.filter { assetPoolRootId(it) == root }
    }

    /** Whether [ledgerId] owns the permanent master asset pool. */
    fun isMasterAssetGroup(ledgerId: Long): Boolean =
        assetPoolRootId(ledgerId) == getMasterLedgerId()

    fun getAssetGroups(): List<AssetGroupDescriptor> {
        val ledgers = getLedgers()
        val masterId = getMasterLedgerId()
        val grouped = ledgers.groupBy { assetPoolRootId(it.id) }
        // How many distinct groups share the same owner name; used only to tell
        // otherwise identical group labels apart in the picker.
        val groupNameCounts = HashMap<String, Int>()
        grouped.forEach { (rootId, members) ->
            val name = members.firstOrNull { it.id == rootId }?.name.orEmpty()
            groupNameCounts[name] = (groupNameCounts[name] ?: 0) + 1
        }
        return grouped
            .map { (rootId, members) ->
                val ordered = members.sortedBy { it.id }
                val rootName = ordered.firstOrNull { it.id == rootId }?.name.orEmpty()
                AssetGroupDescriptor(
                    rootLedgerId = rootId,
                    rootLedgerName = rootName,
                    memberLedgerIds = ordered.map { it.id },
                    memberLedgerNames = ordered.map { it.name },
                    isMaster = rootId == masterId,
                    assetCount = getLedgerAssetPoolAssetCount(rootId),
                    sameNameGroupCount = groupNameCounts[rootName] ?: 1
                )
            }
            .sortedWith(compareBy({ !it.isMaster }, { it.rootLedgerId }))
    }

    private fun getLedgerAssetPoolAssetCount(rootId: Long): Int {
        return readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
            arrayOf(rootId.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Creates a ledger that either starts an empty independent asset group
     * ([sharedSourceLedgerId] null) or joins an existing group. Joining stores no
     * asset copies: the ledger references the group root, so the same asset ids
     * and balances are shared. The whole operation is one transaction, so a
     * failure leaves neither a ledger nor a half-written relationship behind.
     */
    fun createLedgerInAssetGroup(
        name: String,
        sharedSourceLedgerId: Long?,
        iconName: String = "tabler_book"
    ): Long? = ledgerWriteRepository.createLedgerInAssetGroup(name, sharedSourceLedgerId, iconName)

    enum class LedgerMergeFailure {
        NONE,
        SAME_LEDGER,
        MISSING_LEDGER,
        CROSS_GROUP,
        MASTER_AS_SOURCE
    }

    /** Validates an explicit source → target merge without writing anything. */
    fun validateLedgerMerge(sourceLedgerId: Long, targetLedgerId: Long): LedgerMergeFailure =
        ledgerWriteRepository.validateMerge(sourceLedgerId, targetLedgerId)

    fun mergeLedgerInto(sourceLedgerId: Long, targetLedgerId: Long): Boolean =
        ledgerWriteRepository.mergeLedgerInto(sourceLedgerId, targetLedgerId)

    fun getCategoryStatistics(type: Int): Map<String, Double> =
        recordStatisticsRepository.categoryStatistics(type)

    fun getCategoryStatisticsByDateRange(type: Int, startDate: String, endDate: String): Map<String, Double> =
        recordStatisticsRepository.categoryStatisticsByDateRange(type, startDate, endDate)

    fun getMonthlyStatistics(type: Int, year: Int): Map<String, Double> =
        recordStatisticsRepository.monthlyStatistics(type, year)

    fun addAsset(asset: Asset): Long = assetWriteRepository.add(asset)

    fun getAllAssets(ledgerId: Long? = null): List<Asset> = assetReadRepository.getActive(ledgerId)

    fun getArchivedAssets(): List<Asset> = assetReadRepository.getArchived()

    fun archiveAsset(id: Long) = assetWriteRepository.archive(id)

    fun unarchiveAsset(id: Long) = assetWriteRepository.unarchive(id)

    fun updateAsset(asset: Asset): Int = assetWriteRepository.update(asset)

    fun setAssetPinned(id: Long, pinned: Boolean): Int = assetWriteRepository.setPinned(id, pinned)

    fun updateAssetSortOrder(assetIds: List<Long>): Boolean = assetWriteRepository.updateSortOrder(assetIds)

    fun deleteArchivedAsset(id: Long): Boolean = assetWriteRepository.deleteArchived(id)

    fun deleteAsset(id: Long): Boolean = assetWriteRepository.delete(id)

    private fun assetNameIsReferenced(db: SQLiteDatabase, assetName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT 1 FROM (" +
                "SELECT $COLUMN_ASSET_SOURCE FROM $TABLE_RECORDS WHERE $COLUMN_ASSET_SOURCE = ? " +
                "UNION ALL " +
                "SELECT $COLUMN_ASSET_SOURCE FROM $TABLE_RECORD_DELETION_UNDO " +
                "WHERE $COLUMN_ASSET_SOURCE = ? AND $COLUMN_UNDO_EXPIRES_AT > ?" +
                ") LIMIT 1",
            arrayOf(assetName, assetName, System.currentTimeMillis().toString())
        )
        return cursor.use { it.moveToFirst() }
    }

    fun getTotalAssets(): Double = Money.toMajorDouble(getTotalAssetsMinor())

    fun getTotalAssetsMinor(): Long {
        val db = readableDatabase
        val total = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_ASSET_AMOUNT), 0) FROM $TABLE_ASSETS " +
                "WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 0 AND $COLUMN_ASSET_INCLUDE_IN_TOTAL = 1",
            null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        db.close()
        return total
    }

    fun addAiChatSession(
        title: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Long = aiChatRepository.addSession(title, createdAt, updatedAt)

    fun getAiChatSessions(): List<AiChatSession> = aiChatRepository.getSessions()

    fun getAiChatSessionById(id: Long): AiChatSession? = aiChatRepository.getSessionById(id)

    fun updateAiChatSessionTitle(
        id: Long,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int = aiChatRepository.updateSessionTitle(id, title, updatedAt)

    fun touchAiChatSession(id: Long, updatedAt: Long = System.currentTimeMillis()): Int =
        aiChatRepository.touchSession(id, updatedAt)

    fun addAiChatMessage(message: AiChatMessage): Long = aiChatRepository.addMessage(message)

    fun getAiChatMessages(sessionId: Long): List<AiChatMessage> = aiChatRepository.getMessages(sessionId)
}
