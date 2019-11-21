/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import kotlinx.coroutines.Dispatchers
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.OAuthAccount
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import kotlin.coroutines.CoroutineContext

class FxAccountObserver(
    context: Context,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : AccountObserver {
    var hasAccount = false
    var hasSyncedItems = false

    override fun onAuthenticated(account: OAuthAccount, newAccount: Boolean) {
        super.onAuthenticated(account, newAccount)
        hasAccount = newAccount
    }

    override fun onAuthenticationProblems() {
        super.onAuthenticationProblems()
        dispatcher.dispatch(LifecycleAction.ForceAccountReset)
        log.error("Account authentication problem occurred.")
    }

    override fun onLoggedOut() {
        super.onLoggedOut()
        hasAccount = false
    }
}

abstract class AccountException : Exception() {
//    class AccountChangedAfterStartup : AccountException()
}