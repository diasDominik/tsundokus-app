package uk.tsundokus.features.authentication.testing

import uk.tsundokus.core.domain.sync.LocalDataResetter

class FakeLocalDataResetter : LocalDataResetter {
    var resetCount: Int = 0
        private set

    override suspend fun resetLocalData() {
        resetCount++
    }
}
