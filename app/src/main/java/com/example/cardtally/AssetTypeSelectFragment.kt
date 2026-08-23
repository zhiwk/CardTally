package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.cardtally.state.AssetType

class AssetTypeSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_asset_type_select, container, false)

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindType(view, R.id.row_cash, AssetType.CASH)
        bindType(view, R.id.row_bank, AssetType.BANK)
        bindType(view, R.id.row_alipay, AssetType.ALIPAY)
        bindType(view, R.id.row_wechat, AssetType.WECHAT)
        return view
    }

    private fun bindType(view: View, rowId: Int, type: AssetType) {
        view.findViewById<View>(rowId).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddAssetFragment.newInstance(type))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.VISIBLE
    }
}
