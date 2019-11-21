/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.components.concept.sync.AccountObserver

internal class FxAccountObserver : AccountObserver {
}

abstract class AccountException : Exception() {
    class AccountChangedAfterStartup : AccountException()
}