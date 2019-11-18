/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class ItemDetailFragment : BackableFragment(), ItemDetailView {

    private var itemId: String? = null
    private var kebabMenu: ItemDetailOptionMenu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        itemId = arguments?.let {
            ItemDetailFragmentArgs.fromBundle(it).itemId
        }

        this.setHasOptionsMenu(true)
        presenter = ItemDetailPresenter(this, itemId)

        return inflater.inflate(R.layout.fragment_item_detail, container, false)
    }

    override fun onPause() {
        kebabMenu?.dismiss()
        super.onPause()
    }

    override fun onDestroy() {
        kebabMenu?.dismiss()
        super.onDestroy()
    }

    private val errorHelper = NetworkErrorHelper()

    override val usernameCopyClicks: Observable<Unit>
        get() = view!!.btnUsernameCopy.clicks()

    override val usernameFieldClicks: Observable<Unit>
        get() = view!!.inputUsername.clicks()

    override val passwordCopyClicks: Observable<Unit>
        get() = view!!.btnPasswordCopy.clicks()

    override val passwordFieldClicks: Observable<Unit>
        get() = view!!.inputPassword.clicks()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val hostnameClicks: Observable<Unit>
        get() = view!!.inputHostname.clicks()

    override val launchButtonClicks: Observable<Unit>
        get() = view!!.btnHostnameLaunch.clicks()

    override val kebabMenuClicks: Observable<Unit>
        get() = view!!.toolbar.kebabMenuButton.clicks()

    override val editClicks: Observable<Unit> = PublishSubject.create()
    override val deleteClicks: Observable<Unit> = PublishSubject.create()

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    override fun showPopup() {
        val wrapper = ContextThemeWrapper(context, R.style.PopupKebabMenu)
        val popupMenu = PopupMenu(
            wrapper, // context
            this.kebabMenuButton, // anchor
            Gravity.END, // gravity
            R.attr.popupWindowStyle, // styleAttr
            R.style.PopupKebabMenu // styleRes
        )
        popupMenu.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.edit -> {
                    (editClicks as PublishSubject).onNext(Unit)
                    true
                }
                R.id.delete -> {
                    (deleteClicks as PublishSubject).onNext(Unit)
                    true
                }
                else -> false
            }
        }

        popupMenu.inflate(R.menu.item_detail_menu)

        val builder = popupMenu.menu as MenuBuilder
        builder.setOptionalIconsVisible(true)
        popupMenu.show()
    }

    @SuppressLint("InflateParams")
    override fun showPopupWindow() {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_menu, null)
        val wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT

        val popupWindow = PopupWindow(popupView, wrapContent, wrapContent, true)

        val xOffset = resources.getDimensionPixelOffset(R.dimen.popup_menu_xoffset)
        val yOffset = resources.getDimensionPixelOffset(R.dimen.popup_menu_yoffset)

        popupWindow.elevation = resources.getDimension(R.dimen.menu_elevation)
        popupWindow.height = resources.getDimensionPixelSize(R.dimen.popup_menu_height)
//        popupWindow.enterTransition = resources.getAnimation(R.anim.slide_in_bottom)

        popupWindow.contentView = LayoutInflater.from(context).inflate(R.layout.popup_menu, null)

        popupWindow.showAsDropDown(
            this.kebabMenuButton, // anchor
            xOffset,
            yOffset,
            Gravity.END
        )

        // set up item menu
        val contentView = popupWindow.contentView

        val topPadding = resources.getDimensionPixelSize(R.dimen.popup_menu_top_padding)
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.popup_menu_bottom_padding)
        val sidePadding = resources.getDimensionPixelSize(R.dimen.popup_menu_side_padding)

        contentView.setPadding(sidePadding, topPadding, sidePadding, bottomPadding)

        val menuItems = listOf<TextView>(
            contentView.findViewById(R.id.edit),
            contentView.findViewById(R.id.delete)
        )

        for (item in menuItems) {
            item.height = resources.getDimensionPixelSize(R.dimen.popup_menu_item_height)
            item.gravity = Gravity.CENTER_VERTICAL
            item.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.drawable_padding)
            item.setBackgroundColor(resources.getColor(R.color.menu_item_selected, null))
            setListeners(item)
        }
    }

    private fun setListeners(item: TextView) {
        item.setOnClickListener {
            when (it.id) {
                R.id.edit -> {
                    (editClicks as PublishSubject).onNext(Unit)
                }
                R.id.delete -> {
                    (deleteClicks as PublishSubject).onNext(Unit)
                }
                else -> { log.info("Not edit or delete. Id = ${item.id}")}
            }
        }

    }

    private fun updatePasswordVisibility(visible: Boolean) {
        if (visible) {
            inputPassword.transformationMethod = null
            btnPasswordToggle.setImageResource(R.drawable.ic_hide)
        } else {
            inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnPasswordToggle.setImageResource(R.drawable.ic_show)
        }
    }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
        toolbar.entryTitle.text = item.title
        toolbar.entryTitle.gravity = Gravity.CENTER_VERTICAL
        toolbar.contentInsetStartWithNavigation = 0

        inputLayoutPassword.setHintTextAppearance(R.style.PasswordHint)

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false
        inputLayoutUsername.isScrollContainer = false
        view?.isScrollContainer = false

        inputUsername.readOnly = true
        inputLayoutUsername.editText?.ellipsize = TextUtils.TruncateAt.END
        inputUsername.ellipsize = TextUtils.TruncateAt.END
        inputUsername.setSingleLine()

        if (!item.hasUsername) {
            btnUsernameCopy.setColorFilter(resources.getColor(R.color.white_60_percent, null))
            inputUsername.isClickable = false
            inputUsername.isFocusable = false
            inputUsername.setText(R.string.empty_space, TextView.BufferType.NORMAL)
        } else {
            btnUsernameCopy.clearColorFilter()
            inputUsername.isClickable = true
            inputUsername.isFocusable = true
            inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        }

        inputPassword.readOnly = true
        inputPassword.isClickable = true
        inputPassword.isFocusable = true

        inputHostname.readOnly = true
        inputHostname.isClickable = true
        inputHostname.isFocusable = true

        btnHostnameLaunch.isClickable = true

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)

        // effect password visibility state
        updatePasswordVisibility(isPasswordVisible)
    }

    override fun showToastNotification(@StringRes strId: Int) {
        assertOnUiThread()
        val toast = Toast(activity)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layoutInflater.inflate(R.layout.toast_view, this.view as ViewGroup, false)
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 0)
        val v = toast.view.findViewById(R.id.message) as TextView
        v.text = resources.getString(strId)
        toast.show()
    }

    // used for feature flag
    override fun showKebabMenu() {
        toolbar.kebabMenuButton.visibility = View.VISIBLE
        toolbar.kebabMenuButton.isClickable = true
    }

    // used for feature flag
    override fun hideKebabMenu() {
        toolbar.kebabMenuButton.visibility = View.GONE
        toolbar.kebabMenuButton.isClickable = false
    }

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(view!!)
        } else {
            errorHelper.hideNetworkError(view!!, view!!.cardView, R.dimen.hidden_network_error)
        }
    }
}

//    override val retryNetworkConnectionClicks: Observable<Unit>
//        get() = view!!.networkWarning.retryButton.clicks()

var EditText.readOnly: Boolean
    get() = this.isFocusable
    set(readOnly) {
        this.isFocusable = !readOnly
        this.isFocusableInTouchMode = !readOnly
        this.isClickable = !readOnly
        this.isLongClickable = !readOnly
        this.isCursorVisible = !readOnly
        this.inputType = InputType.TYPE_NULL
    }