package uk.tsundokus.features.settings.presentation.deleteaccount

/**
 * The word the user must type to arm account deletion.
 *
 * Deliberately a constant rather than a string resource: the prompt that asks for it *is*
 * translated, and the two are compared for equality. If a translator localised the keyword in one
 * place and not the other — or localised the prompt while the comparison stayed English — the
 * delete button could never enable, and the account would be undeletable in that language.
 * Injecting this into the prompt keeps them in lockstep by construction.
 */
internal const val DELETE_ACCOUNT_CONFIRM_KEYWORD = "DELETE"
