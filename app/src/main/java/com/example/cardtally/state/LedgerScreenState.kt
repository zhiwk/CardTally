package com.example.cardtally.state

import android.os.Bundle

enum class LedgerViewState(val token: String) { DETAILS("details"), STATISTICS_EXPENSE("statistics_expense"), STATISTICS_INCOME("statistics_income") }
enum class StatisticsType(val token: String) { EXPENSE("expense"), INCOME("income") }
enum class PeriodPreset(val token: String) { WEEK("week"), MONTH("month"), YEAR("year"), ALL("all"), CUSTOM("custom") }
enum class ChartMode(val token: String) { PIE("pie"), LINE("line") }
enum class FilterSurface(val token: String) { NONE("none"), PERIOD("period") }

data class LedgerScreenState(
    val view: LedgerViewState,
    val statisticsType: StatisticsType,
    val periodPreset: PeriodPreset,
    val customStartDate: String?,
    val customEndDate: String?,
    val chartMode: ChartMode,
    val detailsScrollPosition: Int,
    val detailsScrollOffset: Int,
    val statisticsScrollPosition: Int,
    val statisticsScrollOffset: Int,
    val openFilterSurface: FilterSurface
) {
    fun writeTo(bundle: Bundle) {
        bundle.putString(KEY_VIEW, view.token)
        bundle.putString(KEY_STATISTICS_TYPE, statisticsType.token)
        bundle.putString(KEY_PERIOD_PRESET, periodPreset.token)
        bundle.putNullableString(KEY_CUSTOM_START_DATE, customStartDate)
        bundle.putNullableString(KEY_CUSTOM_END_DATE, customEndDate)
        bundle.putString(KEY_CHART_MODE, chartMode.token)
        bundle.putInt(KEY_DETAILS_SCROLL_POSITION, detailsScrollPosition)
        bundle.putInt(KEY_DETAILS_SCROLL_OFFSET, detailsScrollOffset)
        bundle.putInt(KEY_STATISTICS_SCROLL_POSITION, statisticsScrollPosition)
        bundle.putInt(KEY_STATISTICS_SCROLL_OFFSET, statisticsScrollOffset)
        bundle.putString(KEY_OPEN_FILTER_SURFACE, openFilterSurface.token)
    }

    companion object {
        private const val KEY_VIEW = "state_view"
        private const val KEY_STATISTICS_TYPE = "state_statistics_type"
        private const val KEY_PERIOD_PRESET = "state_period_preset"
        private const val KEY_CUSTOM_START_DATE = "state_custom_start_date"
        private const val KEY_CUSTOM_END_DATE = "state_custom_end_date"
        private const val KEY_CHART_MODE = "state_chart_mode"
        private const val KEY_DETAILS_SCROLL_POSITION = "state_details_scroll_position"
        private const val KEY_DETAILS_SCROLL_OFFSET = "state_details_scroll_offset"
        private const val KEY_STATISTICS_SCROLL_POSITION = "state_statistics_scroll_position"
        private const val KEY_STATISTICS_SCROLL_OFFSET = "state_statistics_scroll_offset"
        private const val KEY_OPEN_FILTER_SURFACE = "state_open_filter_surface"

        fun readFrom(bundle: Bundle?, startupView: LedgerViewState): LedgerScreenState {
            if (bundle == null) return defaults(startupView)
            var period = PeriodPreset.values().firstOrNull { it.token == bundle.getString(KEY_PERIOD_PRESET) } ?: PeriodPreset.MONTH
            var startDate = bundle.getString(KEY_CUSTOM_START_DATE)
            var endDate = bundle.getString(KEY_CUSTOM_END_DATE)
            if (period == PeriodPreset.CUSTOM && (startDate == null || endDate == null || startDate > endDate)) {
                period = PeriodPreset.ALL
                startDate = null
                endDate = null
            }
            return LedgerScreenState(
                view = LedgerViewState.values().firstOrNull { it.token == bundle.getString(KEY_VIEW) } ?: startupView,
                statisticsType = StatisticsType.values().firstOrNull { it.token == bundle.getString(KEY_STATISTICS_TYPE) } ?: StatisticsType.EXPENSE,
                periodPreset = period,
                customStartDate = startDate,
                customEndDate = endDate,
                chartMode = ChartMode.values().firstOrNull { it.token == bundle.getString(KEY_CHART_MODE) } ?: ChartMode.PIE,
                detailsScrollPosition = bundle.getInt(KEY_DETAILS_SCROLL_POSITION).coerceAtLeast(0),
                detailsScrollOffset = bundle.getInt(KEY_DETAILS_SCROLL_OFFSET),
                statisticsScrollPosition = bundle.getInt(KEY_STATISTICS_SCROLL_POSITION).coerceAtLeast(0),
                statisticsScrollOffset = bundle.getInt(KEY_STATISTICS_SCROLL_OFFSET),
                openFilterSurface = FilterSurface.values().firstOrNull { it.token == bundle.getString(KEY_OPEN_FILTER_SURFACE) } ?: FilterSurface.NONE
            )
        }

        fun defaults(startupView: LedgerViewState) = LedgerScreenState(
            startupView,
            if (startupView == LedgerViewState.STATISTICS_INCOME) StatisticsType.INCOME else StatisticsType.EXPENSE,
            PeriodPreset.MONTH,
            null,
            null,
            ChartMode.PIE,
            0,
            0,
            0,
            0,
            FilterSurface.NONE
        )
    }
}
