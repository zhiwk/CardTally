# Every androidTest class must appear exactly once. The runner executes these
# groups serially against the isolated verification application.
$deviceTestGroups = @(
    @{ Name = 'agent'; Expected = 12; Classes = @(
        'com.example.cardtally.AgentLayoutIsolationTest',
        'com.example.cardtally.AgentReasoningMessageTest',
        'com.example.cardtally.AgentSendLifecycleTest'
    ) },
    @{ Name = 'amount-render'; Expected = 10; Classes = @(
        'com.example.cardtally.AmountKeypadRenderTest',
        'com.example.cardtally.EntryAmountLayoutTest',
        'com.example.cardtally.AssetExclusionBadgeTest'
    ) },
    @{ Name = 'ledger-form'; Expected = 8; Classes = @('com.example.cardtally.LedgerSetupFormTest') },
    @{ Name = 'main-layout'; Expected = 6; Classes = @(
        'com.example.cardtally.MainActivityFabLayoutTest',
        'com.example.cardtally.MainActivityThemeApplicationTest',
        'com.example.cardtally.RecordPresentationTest'
    ) },
    @{ Name = 'record-layout'; Expected = 12; Classes = @('com.example.cardtally.QuickRecordLayoutTest') },
    @{ Name = 'record-sheets'; Expected = 5; Classes = @(
        'com.example.cardtally.RecordAssetSelectionMemoryTest',
        'com.example.cardtally.RecordSheetInteractionTest',
        'com.example.cardtally.RecurringEntryLayoutTest'
    ) },
    @{ Name = 'screen-state'; Expected = 11; Classes = @(
        'com.example.cardtally.RecordRowLayoutMeasurementTest',
        'com.example.cardtally.state.ScreenStateBundleTest'
    ) },
    @{ Name = 'asset-groups'; Expected = 12; Classes = @(
        'com.example.cardtally.database.DatabaseHelperAssetGroupTest',
        'com.example.cardtally.database.DatabaseHelperNegativeAssetTest'
    ) },
    @{ Name = 'category-db'; Expected = 11; Classes = @(
        'com.example.cardtally.database.DatabaseHelperCategoryTreeTest',
        'com.example.cardtally.database.DatabaseHelperRecursiveCategoryQueryTest',
        'com.example.cardtally.database.DatabaseHelperRecursiveCategoryMigrationTest',
        'com.example.cardtally.database.DatabaseHelperBetaSchemaTest'
    ) },
    @{ Name = 'record-db'; Expected = 11; Classes = @(
        'com.example.cardtally.database.DatabaseHelperRecordDeletionTest',
        'com.example.cardtally.database.DatabaseHelperTransferFeeTest'
    ) },
    @{ Name = 'session-recurring-db'; Expected = 9; Classes = @(
        'com.example.cardtally.database.DatabaseHelperAgentChatSessionTest',
        'com.example.cardtally.database.DataTransferManagerTest',
        'com.example.cardtally.database.DatabaseHelperRecurringRecordTest'
    ) },
    @{ Name = 'ledger-db'; Expected = 8; Classes = @(
        'com.example.cardtally.database.DatabaseHelperLedgerAggregationContractTest',
        'com.example.cardtally.database.DatabaseHelperTodayRecordsPagingTest'
    ) },
    @{ Name = 'preferences'; Expected = 15; Classes = @(
        'com.example.cardtally.util.AiAssistantSettingsHelperTest',
        'com.example.cardtally.util.CategoryHierarchySettingsHelperTest',
        'com.example.cardtally.util.LedgerUxPreferencesTest',
        'com.example.cardtally.util.RecordEntryModePreferencesTest',
        'com.example.cardtally.util.ThemeHelperTest'
    ) }
)
