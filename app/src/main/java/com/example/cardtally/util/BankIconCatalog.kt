package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.R

/** Bank identity is persisted in categoryIconName; the card category stays unchanged. */
object BankIconCatalog {
    const val OTHER_ICON_NAME = "ic_bank_other"
    data class Bank(val iconName: String, val nameRes: Int, val iconRes: Int, val section: String)
    val other = Bank(OTHER_ICON_NAME, R.string.bank_other, R.drawable.ic_bank_other, "*")
    val banks = listOf(
        Bank("ic_bank_cmb", R.string.bank_name_cmb, R.drawable.ic_bank_cmb, "Z"),
        Bank("ic_bank_ccb", R.string.bank_name_ccb, R.drawable.ic_bank_ccb, "J"),
        Bank("ic_bank_cgb", R.string.bank_name_cgb, R.drawable.ic_bank_cgb, "G"),
        Bank("ic_bank_icbc", R.string.bank_name_icbc, R.drawable.ic_bank_icbc, "G"),
        Bank("ic_bank_boc", R.string.bank_name_boc, R.drawable.ic_bank_boc, "Z"),
        Bank("ic_bank_comm", R.string.bank_name_comm, R.drawable.ic_bank_comm, "J"),
        Bank("ic_bank_citic", R.string.bank_name_citic, R.drawable.ic_bank_citic, "Z"),
        Bank("ic_bank_pingan", R.string.bank_name_pingan, R.drawable.ic_bank_pingan, "P"),
        Bank("ic_bank_spdb", R.string.bank_name_spdb, R.drawable.ic_bank_spdb, "P"),
        Bank("ic_bank_psbc", R.string.bank_name_psbc, R.drawable.ic_bank_psbc, "Z"),
        Bank("ic_bank_ceb", R.string.bank_name_ceb, R.drawable.ic_bank_ceb, "G"),
        Bank("ic_bank_abc", R.string.bank_name_abc, R.drawable.ic_bank_abc, "N"),
        Bank("ic_bank_cmbc", R.string.bank_name_cmbc, R.drawable.ic_bank_cmbc, "Z"),
        Bank("ic_bank_cib", R.string.bank_name_cib, R.drawable.ic_bank_cib, "X"),
        Bank("ic_bank_arcu", R.string.bank_name_arcu, R.drawable.ic_bank_arcu, "A"),
        Bank("ic_bank_bjb", R.string.bank_name_bjb, R.drawable.ic_bank_bjb, "B"),
        Bank("ic_bank_bohai", R.string.bank_name_bohai, R.drawable.ic_bank_bohai, "B"),
        Bank("ic_bank_csrcb", R.string.bank_name_csrcb, R.drawable.ic_bank_csrcb, "C"),
        Bank("ic_bank_cdrcb", R.string.bank_name_cdrcb, R.drawable.ic_bank_cdrcb, "C"),
        Bank("ic_bank_bocd", R.string.bank_name_bocd, R.drawable.ic_bank_bocd, "C"),
        Bank("ic_bank_deyang", R.string.bank_name_deyang, R.drawable.ic_bank_deyang, "D"),
        Bank("ic_bank_fjhx", R.string.bank_name_fjhx, R.drawable.ic_bank_fjhx, "F"),
        Bank("ic_bank_ganzhou", R.string.bank_name_ganzhou, R.drawable.ic_bank_ganzhou, "G"),
        Bank("ic_bank_bbg", R.string.bank_name_bbg, R.drawable.ic_bank_bbg, "G"),
        Bank("ic_bank_grcb", R.string.bank_name_grcb, R.drawable.ic_bank_grcb, "G"),
        Bank("ic_bank_gz", R.string.bank_name_gz, R.drawable.ic_bank_gz, "G"),
        Bank("ic_bank_guilin", R.string.bank_name_guilin, R.drawable.ic_bank_guilin, "G"),
        Bank("ic_bank_hrb", R.string.bank_name_hrb, R.drawable.ic_bank_hrb, "H"),
        Bank("ic_bank_hebei", R.string.bank_name_hebei, R.drawable.ic_bank_hebei, "H"),
        Bank("ic_bank_hxb", R.string.bank_name_hxb, R.drawable.ic_bank_hxb, "H"),
        Bank("ic_bank_hsbank", R.string.bank_name_hsbank, R.drawable.ic_bank_hsbank, "H"),
        Bank("ic_bank_hld", R.string.bank_name_hld, R.drawable.ic_bank_hld, "H"),
        Bank("ic_bank_jsrcu", R.string.bank_name_jsrcu, R.drawable.ic_bank_jsrcu, "J"),
        Bank("ic_bank_jsb", R.string.bank_name_jsb, R.drawable.ic_bank_jsb, "J"),
        Bank("ic_bank_jxbank", R.string.bank_name_jxbank, R.drawable.ic_bank_jxbank, "J"),
        Bank("ic_bank_jilin", R.string.bank_name_jilin, R.drawable.ic_bank_jilin, "J"),
        Bank("ic_bank_jincheng", R.string.bank_name_jincheng, R.drawable.ic_bank_jincheng, "J"),
        Bank("ic_bank_liuzhou", R.string.bank_name_liuzhou, R.drawable.ic_bank_liuzhou, "L"),
        Bank("ic_bank_longjiang", R.string.bank_name_longjiang, R.drawable.ic_bank_longjiang, "L"),
        Bank("ic_bank_nanjing", R.string.bank_name_nanjing, R.drawable.ic_bank_nanjing, "N"),
        Bank("ic_bank_neimenggu", R.string.bank_name_neimenggu, R.drawable.ic_bank_neimenggu, "N"),
        Bank("ic_bank_ningbo", R.string.bank_name_ningbo, R.drawable.ic_bank_ningbo, "N"),
        Bank("ic_bank_qingdao", R.string.bank_name_qingdao, R.drawable.ic_bank_qingdao, "Q"),
        Bank("ic_bank_qinghai", R.string.bank_name_qinghai, R.drawable.ic_bank_qinghai, "Q"),
        Bank("ic_bank_qishang", R.string.bank_name_qishang, R.drawable.ic_bank_qishang, "Q"),
        Bank("ic_bank_shrcb", R.string.bank_name_shrcb, R.drawable.ic_bank_shrcb, "S"),
        Bank("ic_bank_shanghai", R.string.bank_name_shanghai, R.drawable.ic_bank_shanghai, "S"),
        Bank("ic_bank_shangrao", R.string.bank_name_shangrao, R.drawable.ic_bank_shangrao, "S"),
        Bank("ic_bank_shaoxing", R.string.bank_name_shaoxing, R.drawable.ic_bank_shaoxing, "S"),
        Bank("ic_bank_szrcb", R.string.bank_name_szrcb, R.drawable.ic_bank_szrcb, "S"),
        Bank("ic_bank_shunde", R.string.bank_name_shunde, R.drawable.ic_bank_shunde, "S"),
        Bank("ic_bank_tianfu", R.string.bank_name_tianfu, R.drawable.ic_bank_tianfu, "S"),
        Bank("ic_bank_suzhou", R.string.bank_name_suzhou, R.drawable.ic_bank_suzhou, "S"),
        Bank("ic_bank_taian", R.string.bank_name_taian, R.drawable.ic_bank_taian, "T"),
        Bank("ic_bank_weifang", R.string.bank_name_weifang, R.drawable.ic_bank_weifang, "W"),
        Bank("ic_bank_wenzhou", R.string.bank_name_wenzhou, R.drawable.ic_bank_wenzhou, "W"),
        Bank("ic_bank_urumqi", R.string.bank_name_urumqi, R.drawable.ic_bank_urumqi, "W"),
        Bank("ic_bank_xiamen", R.string.bank_name_xiamen, R.drawable.ic_bank_xiamen, "W"),
        Bank("ic_bank_xingtai", R.string.bank_name_xingtai, R.drawable.ic_bank_xingtai, "X"),
        Bank("ic_bank_yingkou", R.string.bank_name_yingkou, R.drawable.ic_bank_yingkou, "Y"),
        Bank("ic_bank_zhangjiagang", R.string.bank_name_zhangjiagang, R.drawable.ic_bank_zhangjiagang, "Z"),
        Bank("ic_bank_zhangjiakou", R.string.bank_name_zhangjiakou, R.drawable.ic_bank_zhangjiakou, "Z"),
        Bank("ic_bank_zhengzhou", R.string.bank_name_zhengzhou, R.drawable.ic_bank_zhengzhou, "Z"),
    )
    private val byIconName = (banks + other).associateBy { it.iconName }
    private val iconResources = byIconName.values.map { it.iconRes }.toSet()
    val common = banks.take(14)

    fun isBankIcon(resource: Int): Boolean = resource in iconResources

    fun forCard(label: String, iconName: String, type: Int): Bank? {
        if (!isCard(label, type)) return null
        return byIconName[iconName] ?: other
    }

    fun isCard(label: String, type: Int): Boolean =
        label in setOf("储蓄卡", "银行卡", "信用卡") || (label.isBlank() && type == 1)

    fun cardLabel(context: Context, label: String): String = context.getString(
        if (label == "信用卡") R.string.bank_card_credit else R.string.bank_card_savings
    )

    fun displayLabel(context: Context, category: String, bank: Bank): String = context.getString(
        R.string.bank_asset_type_format, context.getString(bank.nameRes), cardLabel(context, category)
    )
}
