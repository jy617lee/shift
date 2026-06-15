import Foundation

final class UserDefaultsPreferencesRepository: UserPreferencesRepository {
    private let defaults: UserDefaults

    private enum Keys {
        static let skipConfirm = "skip_confirm"
        static let skipConfirmPromptShown = "skip_confirm_prompt_shown"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func isSkipConfirm() -> Bool {
        defaults.bool(forKey: Keys.skipConfirm)
    }

    func setSkipConfirm(_ value: Bool) {
        defaults.set(value, forKey: Keys.skipConfirm)
    }

    func isSkipConfirmPromptShown() -> Bool {
        defaults.bool(forKey: Keys.skipConfirmPromptShown)
    }

    func setSkipConfirmPromptShown(_ value: Bool) {
        defaults.set(value, forKey: Keys.skipConfirmPromptShown)
    }
}
